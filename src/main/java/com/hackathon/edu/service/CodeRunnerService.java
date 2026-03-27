package com.hackathon.edu.service;

import com.hackathon.edu.config.AppCodeRunnerProperties;
import com.hackathon.edu.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CodeRunnerService {
    public static final String LANGUAGE_JAVA = "java";
    public static final String LANGUAGE_BASH = "bash";

    private static final String JAVA_IMAGE = "eclipse-temurin:21-jdk";
    private static final String BASH_IMAGE = "bash:5.2";

    private final AppCodeRunnerProperties properties;

    public ExecutionResult run(String language, String code, String inputData) {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "runner_unavailable");
        }

        validateInput(code, inputData);

        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("edu-runner-");
            Path stdinFile = workspace.resolve("stdin.txt");
            Path stdoutFile = workspace.resolve("stdout.txt");
            Path stderrFile = workspace.resolve("stderr.txt");
            Path dockerStdoutFile = workspace.resolve("docker.stdout.txt");
            Path dockerStderrFile = workspace.resolve("docker.stderr.txt");

            Files.writeString(stdinFile, inputData == null ? "" : inputData, StandardCharsets.UTF_8);
            writeCodeFile(workspace, language, code);

            List<String> command = buildDockerCommand(workspace, language);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(dockerStdoutFile.toFile());
            pb.redirectError(dockerStderrFile.toFile());

            long startedNs = System.nanoTime();
            Process process = pb.start();
            boolean finished = process.waitFor(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                finished = process.waitFor(1, TimeUnit.SECONDS);
            }
            long durationMs = Duration.ofNanos(System.nanoTime() - startedNs).toMillis();

            if (!Files.exists(stdoutFile) || !Files.exists(stderrFile)) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "runner_unavailable");
            }

            String stdout = readLimited(stdoutFile, properties.getMaxOutputLength());
            String stderr = readLimited(stderrFile, properties.getMaxOutputLength());
            String dockerStderr = readLimited(dockerStderrFile, properties.getMaxOutputLength());

            Integer exitCode = finished ? process.exitValue() : null;
            String status = resolveStatus(language, finished, exitCode, stderr, dockerStderr);
            return new ExecutionResult(status, stdout, stderr, exitCode, !finished, durationMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "runner_unavailable");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "runner_unavailable");
        } finally {
            deleteDirectoryQuietly(workspace);
        }
    }

    private void validateInput(String code, String inputData) {
        if (code == null || code.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "code_invalid");
        }
        if (code.length() > properties.getMaxCodeLength()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "code_too_large");
        }
        if (inputData != null && inputData.length() > properties.getMaxInputLength()) {
            throw new ApiException(HttpStatus.CONFLICT, "task_input_too_large");
        }
    }

    private void writeCodeFile(Path workspace, String language, String code) throws IOException {
        if (LANGUAGE_JAVA.equals(language)) {
            Files.writeString(workspace.resolve("Main.java"), code, StandardCharsets.UTF_8);
            return;
        }
        if (LANGUAGE_BASH.equals(language)) {
            Files.writeString(workspace.resolve("script.sh"), code, StandardCharsets.UTF_8);
            return;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "runner_language_not_supported");
    }

    private List<String> buildDockerCommand(Path workspace, String language) {
        String image;
        String runCommand;
        long outputLimitBlocks = maxOutputBlocks(properties.getMaxOutputLength());
        if (LANGUAGE_JAVA.equals(language)) {
            image = JAVA_IMAGE;
            runCommand = "ulimit -f " + outputLimitBlocks
                    + "; : > stdout.txt; : > stderr.txt; "
                    + "javac Main.java 2> stderr.txt; rc=$?; "
                    + "if [ $rc -ne 0 ]; then exit $rc; fi; "
                    + "java Main < stdin.txt > stdout.txt 2> stderr.txt";
        } else if (LANGUAGE_BASH.equals(language)) {
            image = BASH_IMAGE;
            runCommand = "ulimit -f " + outputLimitBlocks
                    + "; : > stdout.txt; : > stderr.txt; "
                    + "bash script.sh < stdin.txt > stdout.txt 2> stderr.txt";
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "runner_language_not_supported");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getDockerCommand());
        cmd.add("run");
        cmd.add("--rm");
        cmd.add("--network");
        cmd.add("none");
        cmd.add("--memory");
        cmd.add(properties.getMemoryMb() + "m");
        cmd.add("--cpus");
        cmd.add(Double.toString(properties.getCpus()));
        cmd.add("--pids-limit");
        cmd.add(Integer.toString(properties.getPidsLimit()));
        cmd.add("--read-only");
        cmd.add("--tmpfs");
        cmd.add("/tmp:rw,size=64m");
        cmd.add("--security-opt");
        cmd.add("no-new-privileges");
        cmd.add("--cap-drop");
        cmd.add("ALL");
        cmd.add("--mount");
        cmd.add("type=bind,source=" + workspace.toAbsolutePath() + ",target=/workspace");
        cmd.add("--workdir");
        cmd.add("/workspace");
        cmd.add(image);
        cmd.add("sh");
        cmd.add("-lc");
        cmd.add(runCommand);
        return cmd;
    }

    private long maxOutputBlocks(int maxOutputLength) {
        long bytes = Math.max(4096L, (long) maxOutputLength * 4L);
        return (bytes + 511L) / 512L;
    }

    private String resolveStatus(
            String language,
            boolean finished,
            Integer exitCode,
            String stderr,
            String dockerStderr
    ) {
        if (!finished) {
            return "timeout";
        }

        if (exitCode != null && exitCode == 0) {
            return "ok";
        }

        String dockerStderrNormalized = dockerStderr == null ? "" : dockerStderr.toLowerCase();
        if (dockerStderrNormalized.contains("cannot connect to the docker daemon")
                || dockerStderrNormalized.contains("executable file not found")
                || dockerStderrNormalized.contains("is not recognized")) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "runner_unavailable");
        }

        if (LANGUAGE_JAVA.equals(language) && looksLikeCompileError(stderr)) {
            return "compile_error";
        }
        return "runtime_error";
    }

    private boolean looksLikeCompileError(String stderr) {
        if (stderr == null || stderr.isBlank()) {
            return false;
        }
        String value = stderr.toLowerCase();
        return value.contains("main.java") && value.contains("error");
    }

    private String readLimited(Path path, int maxChars) throws IOException {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            char[] chunk = new char[1024];
            while (sb.length() <= maxChars) {
                int read = reader.read(chunk);
                if (read < 0) {
                    break;
                }
                sb.append(chunk, 0, read);
            }
        }
        if (sb.length() > maxChars) {
            return sb.substring(0, maxChars);
        }
        return sb.toString();
    }

    private void deleteDirectoryQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignore) {
                        }
                    });
        } catch (IOException ignore) {
        }
    }

    public record ExecutionResult(
            String status,
            String stdout,
            String stderr,
            Integer exitCode,
            boolean timedOut,
            long durationMs
    ) {
    }
}
