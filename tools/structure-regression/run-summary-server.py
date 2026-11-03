from pathlib import Path
import os
import shutil
import subprocess
import time

root = Path(__file__).resolve().parents[2]
directory = root / 'build/summary-server-audit'
java_home = os.environ.get('JAVA_HOME')
java = str(Path(java_home) / 'bin/java.exe') if java_home else shutil.which('java')
if not java:
    raise SystemExit('Set JAVA_HOME to a Java 17 installation or add java to PATH')

# 使用已准备的隔离服务端；启动完成后发送正常停服命令。
with (directory / 'console.log').open('w', encoding='utf-8') as log:
    process = subprocess.Popen(
        [java, '-Xms512M', '-Xmx2G',
         '@libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt', 'nogui'],
        cwd=directory, stdin=subprocess.PIPE, stdout=log,
        stderr=subprocess.STDOUT, text=True,
    )
    try:
        deadline = time.time() + 180
        while process.poll() is None and time.time() < deadline:
            text = (directory / 'console.log').read_text(encoding='utf-8', errors='replace')
            if 'Done (' in text:
                process.stdin.write('stop\n')
                process.stdin.flush()
                process.wait(timeout=60)
                assert process.returncode == 0, process.returncode
                (directory / 'result.txt').write_text(
                    'PASS dedicated server reached Done and stopped cleanly', encoding='utf-8',
                )
                break
            time.sleep(1)
        else:
            raise RuntimeError('Server failed or timed out')
    finally:
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=15)
