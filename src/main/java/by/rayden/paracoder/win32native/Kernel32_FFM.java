package by.rayden.paracoder.win32native;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class Kernel32_FFM {
    public static final Kernel32_FFM INSTANCE = new Kernel32_FFM();

    private final MethodHandle getCommandLineW_MH;
    private final MethodHandle localFree_MH;

    private Kernel32_FFM() {
        // 1. Получаем линкер и ищем библиотеку kernel32
        Linker linker = Linker.nativeLinker();
        SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());

        // 2. Описываем и создаем дескриптор функции GetCommandLineW
        // Она не принимает аргументов и возвращает адрес (указатель)
        this.getCommandLineW_MH = linker.downcallHandle(kernel32.find("GetCommandLineW").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );

        // Описываем и создаем дескриптор функции LocalFree
        // Она принимает и возвращает адрес (указатель)
        this.localFree_MH = linker.downcallHandle(kernel32.find("LocalFree").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    }

    /**
     * Retrieves the command-line string for the current process.
     * The lifetime of the returned value is managed by the system, applications should not free or modify this value.
     * <p>
     * To convert the command line to an argv style array of strings,
     * pass the result from {@code GetCommandLineW} to {@code CommandLineToArgvW}.
     *
     * @return the command-line string for the current process.
     */
    public String getCommandLineW() {
        try {
            MemorySegment segment = (MemorySegment) this.getCommandLineW_MH.invoke();

            // 4. Читаем UTF-16 (Wide) строку из полученного адреса
            // Так как GetCommandLineW возвращает null-terminated строку,
            // нам нужно интерпретировать её размер перед чтением.
            return segment.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_16LE);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Frees the specified local memory object and invalidates its handle.
     *
     * @param ptr A handle to the local memory object. If the <tt>pointer</tt> parameter
     *             is NULL, {@code LocalFree} ignores the parameter and returns NULL.
     * @return If the function succeeds, the return value is NULL. If the
     * function fails, the return value is equal to a handle to the
     * local memory object. To get extended error information, call
     * {@code GetLastError}.
     * @see <A HREF="https://msdn.microsoft.com/en-us/library/windows/desktop/aa366730(v=vs.85).aspx">LocalFree</A>
     */
    public MemorySegment localFree(MemorySegment ptr) {
        try {
            return (MemorySegment) this.localFree_MH.invoke(ptr);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
