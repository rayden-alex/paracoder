package by.rayden.paracoder.win32native;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

interface Kernel32 extends StdCallLibrary {
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * Retrieves the command-line string for the current process.
     * The lifetime of the returned value is managed by the system, applications should not free or modify this value.
     * <p>
     * To convert the command line to an argv style array of strings,
     * pass the result from GetCommandLineW to CommandLineToArgvW.
     *
     * @return the command-line string for the current process.
     */
    WString GetCommandLineW();

    /**
     * Frees the specified local memory object and invalidates its handle.
     *
     * @param pointer A handle to the local memory object. If the <tt>pointer</tt> parameter
     *             is NULL, {@code LocalFree} ignores the parameter and returns NULL.
     * @return If the function succeeds, the return value is NULL. If the
     * function fails, the return value is equal to a handle to the
     * local memory object. To get extended error information, call
     * {@code GetLastError}.
     * @see <A HREF="https://msdn.microsoft.com/en-us/library/windows/desktop/aa366730(v=vs.85).aspx">LocalFree</A>
     */
    Pointer LocalFree(Pointer pointer);
}
