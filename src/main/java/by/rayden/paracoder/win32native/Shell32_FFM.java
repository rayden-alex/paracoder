package by.rayden.paracoder.win32native;

import org.springframework.lang.NonNull;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

@SuppressWarnings({"SpellCheckingInspection", "unused"})
public class Shell32_FFM {
    public static final Shell32_FFM INSTANCE = new Shell32_FFM();

    private final MethodHandle commandLineToArgvW_MH;
    private final MethodHandle shFileOperationW_MH;

    private Shell32_FFM() {
        Linker linker = Linker.nativeLinker();
        SymbolLookup shell32 = SymbolLookup.libraryLookup("shell32.dll", Arena.global());

        // Описываем сигнатуру: LPWSTR* CommandLineToArgvW(LPCWSTR lpCmdLine, int* pNumArgs)
        this.commandLineToArgvW_MH = linker.downcallHandle(shell32.findOrThrow("CommandLineToArgvW"),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        // Получаем адрес функции: int SHFileOperation(SHFILEOPSTRUCT fileOp)
        this.shFileOperationW_MH = linker.downcallHandle(shell32.findOrThrow("SHFileOperationW"),
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    }

    //@formatter:off
    private static final int FO_MOVE = 0x0001;
    private static final int FO_COPY = 0x0002;
    private static final int FO_DELETE = 0x0003;
    private static final int FO_RENAME = 0x0004;

    private static final int FOF_MULTIDESTFILES = 0x0001;
    private static final int FOF_CONFIRMMOUSE = 0x0002;
    private static final int FOF_SILENT = 0x0004; // don't display progress UI (confirm prompts may be displayed still)
    private static final int FOF_RENAMEONCOLLISION = 0x0008; // automatically rename the source files to avoid the collisions
    private static final int FOF_NOCONFIRMATION = 0x0010; // don't display confirmation UI, assume "yes" for cases that can be bypassed, "no" for those that can not
    private static final int FOF_WANTMAPPINGHANDLE = 0x0020; // Fill in SHFILEOPSTRUCT.hNameMappings
    private static final int FOF_ALLOWUNDO = 0x0040; // enable undo including Recycle behavior for IFileOperation::Delete()
    private static final int FOF_FILESONLY = 0x0080; // only operate on the files (non folders), both files and folders are assumed without this
    private static final int FOF_SIMPLEPROGRESS = 0x0100; // means don't show names of files
    private static final int FOF_NOCONFIRMMKDIR = 0x0200; // don't dispplay confirmatino UI before making any needed directories, assume "Yes" in these cases
    private static final int FOF_NOERRORUI = 0x0400; // don't put up error UI, other UI may be displayed, progress, confirmations
    private static final int FOF_NOCOPYSECURITYATTRIBS = 0x0800; // dont copy file security attributes (ACLs)
    private static final int FOF_NORECURSION = 0x1000; // don't recurse into directories for operations that would recurse
    private static final int FOF_NO_CONNECTED_ELEMENTS = 0x2000; // don't operate on connected elements ("xxx_files" folders that go with .htm files)
    private static final int FOF_WANTNUKEWARNING = 0x4000; // during delete operation, warn if nuking instead of recycling (partially overrides FOF_NOCONFIRMATION)
    private static final int FOF_NORECURSEREPARSE = 0x8000; // deprecated; the operations engine always does the right thing on FolderLink objects (symlinks, reparse points, folder shortcuts)
    private static final int FOF_NO_UI = (FOF_SILENT | FOF_NOCONFIRMATION | FOF_NOERRORUI | FOF_NOCONFIRMMKDIR); // don't display any UI at all

    private static final int PO_DELETE = 0x0013; // printer is being deleted
    private static final int PO_RENAME = 0x0014; // printer is being renamed
    private static final int PO_PORTCHANGE = 0x0020; // port this printer connected to is being changed
    private static final int PO_REN_PORT = 0x0034; // PO_RENAME and PO_PORTCHANGE at same time.
    //@formatter:on

    /**
     * SHFILEOPSTRUCTW Layout (Windows x64)
     * <pre>
     * typedef struct _SHFILEOPSTRUCTW {
     *   HWND            hwnd;              // 8 bytes
     *   UINT            wFunc;             // 4 bytes
     *   PCZZWSTR        pFrom;             // 8 bytes
     *   PCZZWSTR        pTo;               // 8 bytes
     *   FILEOP_FLAGS    fFlags;            // 2 bytes (WORD)
     *   BOOL            fAnyOperationsAborted; // 4 bytes
     *   LPVOID          hNameMappings;     // 8 bytes
     *   PCWSTR          lpszProgressTitle; // 8 bytes
     * } SHFILEOPSTRUCTW;
     * <pre/>
     * В архитектуре x64 указатели должны быть выровнены по 8 байтам.
     * <p>
     * Только указатели ????? Надо еще уточнить!
     */
    private final StructLayout SHFILEOPSTRUCT_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("hwnd"),                   // HWND
        ValueLayout.JAVA_INT.withName("wFunc"),                 // UINT (FO_DELETE = 3)
        MemoryLayout.paddingLayout(4),                  // padding
        ValueLayout.ADDRESS.withName("pFrom"),                  // PCZZWSTR (двойной нуль-терминатор)
        ValueLayout.ADDRESS.withName("pTo"),                    // PCZZWSTR
        ValueLayout.JAVA_SHORT.withName("fFlags"),              // FILEOP_FLAGS (FOF_ALLOWUNDO = 0x40)
        MemoryLayout.paddingLayout(2),                  // padding
        ValueLayout.JAVA_INT.withName("fAnyOperationsAborted"), // BOOL: This is usually a typedef for an integer type
//        MemoryLayout.paddingLayout(2),                  // padding
        ValueLayout.ADDRESS.withName("hNameMappings"),          // LPVOID
        ValueLayout.ADDRESS.withName("lpszProgressTitle")       // PCWSTR
    ).withByteAlignment(8);

    // Создаем VarHandle для доступа к полям через именованные элементы макета
    private final VarHandle HWND_VH = getVarHandle("hwnd");
    private final VarHandle WFUNC_VH = getVarHandle("wFunc");
    private final VarHandle PFROM_VH = getVarHandle("pFrom");
    private final VarHandle PTO_VH = getVarHandle("pTo");
    private final VarHandle FFLAGS_VH = getVarHandle("fFlags");
    private final VarHandle FANYOPERATIONSABORTED_VH = getVarHandle("fAnyOperationsAborted");

    private VarHandle getVarHandle(String name) {
        return this.SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement(name));
    }


    /**
     * Разбирает строку командной строки в список аргументов.
     */
    public List<String> commandLineToArgvW(String commandLine) {
        // Используем Confined Arena для автоматического управления памятью в блоке
        try (Arena arena = Arena.ofConfined()) {
            // 1. Аллоцируем входную строку как LPCWSTR (UTF-16 с нулевым терминатором)
            // Добавляем нулевой терминатор вручную и используем правильную кодировку.
            // allocateFrom(String) для UTF_16LE обычно требует явного нуля в конце.
            MemorySegment lpCmdLineWithNull = arena.allocateFrom(commandLine + "\0", StandardCharsets.UTF_16LE);

            // 2. Аллоцируем память под выходной параметр pNumArgs (int)
            MemorySegment pNumArgs = arena.allocate(ValueLayout.JAVA_INT);

            // 3. Вызываем нативную функцию
            // returns an array of pointers to the command line arguments, along with a count of such arguments
            // Важно: память, выделенную CommandLineToArgvW, нужно освобождать через LocalFree(pArgv).
            // The MemorySegment API uses zero-length memory segments to represent pointers
            // returned from a foreign function;
            MemorySegment pArgv = (MemorySegment) this.commandLineToArgvW_MH.invoke(lpCmdLineWithNull, pNumArgs);

            if (pArgv.equals(MemorySegment.NULL)) {
                throw new RuntimeException("FFM CommandLineToArgvW failed");
            }

            return getArgList(pArgv, pNumArgs);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /// Convert an array of pointers to the command line arguments to string list.
    @NonNull
    private List<String> getArgList(MemorySegment pArgv, MemorySegment pNumArgs) {
        try {
            int numArgs = pNumArgs.get(ValueLayout.JAVA_INT, 0);

            // Размер массива указателей = количество аргументов * размер указателя (обычно 8 байт)
            long arrayByteSize = (long) numArgs * ValueLayout.ADDRESS.byteSize();

            // "Переинтерпретируем" адрес в сегмент нужного размера
            MemorySegment pArgvArray = pArgv.reinterpret(arrayByteSize);

            return IntStream.range(0, numArgs)
                            .mapToObj(i -> pArgvArray.getAtIndex(ValueLayout.ADDRESS, i))
                            .map(this::getUtf16String)
                            .toList();
        } finally {
            Kernel32_FFM.INSTANCE.localFree(pArgv);
        }
    }

    /// Читаем строку до нулевого символа
    private String getUtf16String(MemorySegment strPtr) {
        // The returned zero-length memory segment cannot be accessed directly by the client: since the size of the
        // segment is zero, any access operation would result in out-of-bounds access. Instead, the client must,
        // unsafely, assign new spatial bounds to the zero-length memory segment.
        // This can be done via the reinterpret(long) method.
        // The maximum length of a command line in Windows is generally either 8,191 or 32,767 characters,
        // depending on how the process is launched.
        return strPtr.reinterpret(Short.MAX_VALUE).getString(0, StandardCharsets.UTF_16LE);
//        return strPtr.getString(0, StandardCharsets.UTF_16LE);
    }


    public int shFileOperation(String... paths) {
        try (Arena arena = Arena.ofConfined()) {
            // Выделяем память под структуру
            MemorySegment pStruct = arena.allocate(this.SHFILEOPSTRUCT_LAYOUT);

            // Подготовка строк
            byte[] pathBytes = encodePaths(paths).getBytes(StandardCharsets.UTF_16LE);
            MemorySegment pFrom = arena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);

            // Заполнение полей ПО ИМЕНИ через VarHandle
            this.HWND_VH.set(pStruct, 0L, MemorySegment.NULL);           // hwnd
            this.WFUNC_VH.set(pStruct, 0L, FO_DELETE);                   // wFunc = FO_DELETE
            this.PFROM_VH.set(pStruct, 0L, pFrom);                       // pFrom
            this.PTO_VH.set(pStruct, 0L, MemorySegment.NULL);            // pTo
            this.FFLAGS_VH.set(pStruct, 0L, (short) (FOF_ALLOWUNDO | FOF_NO_UI)); // fFlags = FOF_ALLOWUNDO

            return (int) this.shFileOperationW_MH.invokeExact(pStruct);
            //            int isAborted = (int)this.FANYOPERATIONSABORTED_VH.get(pStruct, 0L);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Although "pFrom" member is declared as a single null-terminated string, it is actually a buffer that can hold
     * multiple null-delimited file names. Each file name is terminated by a single NULL character. The last file
     * name is terminated with a double NULL character ("\0\0") to indicate the end of the buffer.
     */
    private String encodePaths(String[] paths) {
        StringBuilder encoded = new StringBuilder();
        for (String path : paths) {
            encoded.append(path);
            encoded.append('\0'); // Member delimiter
        }
        encoded.append('\0'); // End of members array
        return encoded.toString();
    }

}
