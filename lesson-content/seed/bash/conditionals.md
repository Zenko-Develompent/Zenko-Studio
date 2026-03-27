# Условные операторы в Bash

В Bash ветвление делают через `if ... then ... else ... fi`.

```bash
if [ "$n" -gt 0 ]; then
  echo "YES"
else
  echo "NO"
fi
```

Не забывайте про пробелы внутри `[ ... ]`.
