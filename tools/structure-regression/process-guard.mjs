import {spawn} from 'node:child_process';
// Own only the child tree launched by this invocation. Never kill Java by image name.
export function guardProcess(child, {timeoutMs, onStop = () => {}}) {
    let reason = null;
    let finished = false;
    function stop(value) {
        if (finished || reason) return;
        reason = value;
        onStop(value);
        if (!child.pid) return;
        if (process.platform === 'win32') {
            const killer = spawn('taskkill.exe', ['/PID', String(child.pid), '/T', '/F'], {windowsHide: true, stdio: 'ignore'});
            killer.on('error', error => onStop('cleanup-error: ' + error.message));
        } else child.kill('SIGKILL');
    }
    const interrupt = () => stop('SIGINT');
    const terminate = () => stop('SIGTERM');
    process.on('SIGINT', interrupt);
    process.on('SIGTERM', terminate);
    const timer = setTimeout(() => stop('timeout'), timeoutMs);
    child.once('close', () => {
        finished = true;
        clearTimeout(timer);
        process.removeListener('SIGINT', interrupt);
        process.removeListener('SIGTERM', terminate);
    });
    return {stop, get reason() {return reason;}};
}
