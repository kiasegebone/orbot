package org.torproject.android.service.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.jaredrummler.android.shell.CommandResult;
import com.jaredrummler.android.shell.Shell;
import com.jaredrummler.android.shell.ShellExitCode;
import com.jaredrummler.android.shell.StreamGobbler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CustomShell extends Shell {


    @WorkerThread
    public static CommandResult run(@NonNull String shell, boolean waitFor, @Nullable Map<String, String> env, @NonNull String command) {
        List<String> stdout = Collections.synchronizedList(new ArrayList<>());
        List<String> stderr = Collections.synchronizedList(new ArrayList<>());
        int exitCode = -1;

        try {

            // setup our process, retrieve stdin stream, and stdout/stderr gobblers
            //Process process = runWithEnv(command, env);
            ProcessBuilder builder = new ProcessBuilder();

            if (env != null && (!env.isEmpty()))
                builder.environment().putAll(env);

            builder.command("/system/bin/" + shell, "-c", command);
            Process process = builder.start();

            StreamGobbler stdoutGobbler = null;
            StreamGobbler stderrGobbler = null;

            if (waitFor) {
                stdoutGobbler = new StreamGobbler(process.getInputStream(), stdout);
                stderrGobbler = new StreamGobbler(process.getErrorStream(), stderr);

                // start gobbling and write our commands to the shell
                stdoutGobbler.start();
                stderrGobbler.start();
            }

            // wait for our process to finish, while we gobble away in the background
            if (waitFor)
                exitCode = process.waitFor();
            else
                exitCode = 0;

            // make sure our threads are done gobbling, our streams are closed, and the process is destroyed - while the
            // latter two shouldn't be needed in theory, and may even produce warnings, in "normal" Java they are required
            // for guaranteed cleanup of resources, so lets be safe and do this on Android as well
            /**
             try {
             stdin.close();
             } catch (IOException e) {
             // might be closed already
             }**/

            if (waitFor) {
                /* ********OpenRefactory Warning********
				 Possible null pointer Dereference!
				 Path: 
					File: CustomShell.java, Line: 39
						StreamGobbler stdoutGobbler=null;
						Variable stdoutGobbler is initialized null.
					File: CustomShell.java, Line: 43
						stdoutGobbler=new StreamGobbler(process.getInputStream(),stdout);
						Variable stdoutGobbler is allocated.
					File: CustomShell.java, Line: 68
						stdoutGobbler.join();
						stdoutGobbler is referenced in method invocation.
				*/
				stdoutGobbler.join();
                stderrGobbler.join();
            }

        } catch (InterruptedException e) {
            exitCode = ShellExitCode.WATCHDOG_EXIT;
        } catch (IOException e) {
            exitCode = ShellExitCode.SHELL_WRONG_UID;
        }

        return new CommandResult(stdout, stderr, exitCode);
    }
}
