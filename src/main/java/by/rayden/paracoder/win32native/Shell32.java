package by.rayden.paracoder.win32native;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import static com.sun.jna.Structure.FieldOrder;

/**
 * Some code for this class is copied from the net.java.dev.jna:jna-platform libraryю
 * I didn't want to include it entirely, so I used only the minimum necessary.
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
interface Shell32 extends StdCallLibrary {
    Shell32 INSTANCE = Native.load("shell32", Shell32.class, W32APIOptions.DEFAULT_OPTIONS);

    //@formatter:off
    int FO_MOVE = 0x0001;
    int FO_COPY = 0x0002;
    int FO_DELETE = 0x0003;
    int FO_RENAME = 0x0004;

    int FOF_MULTIDESTFILES = 0x0001;
    int FOF_CONFIRMMOUSE = 0x0002;
    int FOF_SILENT = 0x0004; // don't display progress UI (confirm prompts may be displayed still)
    int FOF_RENAMEONCOLLISION = 0x0008; // automatically rename the source files to avoid the collisions
    int FOF_NOCONFIRMATION = 0x0010; // don't display confirmation UI, assume "yes" for cases that can be bypassed, "no" for those that can not
    int FOF_WANTMAPPINGHANDLE = 0x0020; // Fill in SHFILEOPSTRUCT.hNameMappings
    int FOF_ALLOWUNDO = 0x0040; // enable undo including Recycle behavior for IFileOperation::Delete()
    int FOF_FILESONLY = 0x0080; // only operate on the files (non folders), both files and folders are assumed without this
    int FOF_SIMPLEPROGRESS = 0x0100; // means don't show names of files
    int FOF_NOCONFIRMMKDIR = 0x0200; // don't dispplay confirmatino UI before making any needed directories, assume "Yes" in these cases
    int FOF_NOERRORUI = 0x0400; // don't put up error UI, other UI may be displayed, progress, confirmations
    int FOF_NOCOPYSECURITYATTRIBS = 0x0800; // dont copy file security attributes (ACLs)
    int FOF_NORECURSION = 0x1000; // don't recurse into directories for operations that would recurse
    int FOF_NO_CONNECTED_ELEMENTS = 0x2000; // don't operate on connected elements ("xxx_files" folders that go with .htm files)
    int FOF_WANTNUKEWARNING = 0x4000; // during delete operation, warn if nuking instead of recycling (partially overrides FOF_NOCONFIRMATION)
    int FOF_NORECURSEREPARSE = 0x8000; // deprecated; the operations engine always does the right thing on FolderLink objects (symlinks, reparse points, folder shortcuts)
    int FOF_NO_UI = (FOF_SILENT | FOF_NOCONFIRMATION | FOF_NOERRORUI | FOF_NOCONFIRMMKDIR); // don't display any UI at all

    int PO_DELETE = 0x0013; // printer is being deleted
    int PO_RENAME = 0x0014; // printer is being renamed
    int PO_PORTCHANGE = 0x0020; // port this printer connected to is being changed
    int PO_REN_PORT = 0x0034; // PO_RENAME and PO_PORTCHANGE at same time.
    //@formatter:on

    /**
     * Parses a Unicode command line string and returns an array of pointers to the
     * command line arguments, along with a count of such arguments, in a way that
     * is similar to the standard C run-time {@code argv} and {@code argc} values.
     *
     * @param lpCmdLine A Unicode string that contains the full command line. If this
     *                  parameter is an empty string the function returns the path to the
     *                  current executable file.
     * @param pNumArgs  Pointer to an {@code int} that receives the number of array
     *                  elements returned, similar to {@code argc}.
     * @return A pointer to an array of WTypes.LPWSTR values, similar to
     * {@code argv}. If the function fails, the return value is
     * {@code null}. To get extended error information, call Kernel32#GetLastError. <br>
     * CommandLineToArgvW allocates a block of contiguous memory for
     * pointers to the argument strings, and for the argument strings
     * themselves; the calling application must free the memory used by the
     * argument list when it is no longer needed. To free the memory, use a
     * single call to the {@link Kernel32#LocalFree} function.
     */
    Pointer CommandLineToArgvW(WString lpCmdLine, IntByReference pNumArgs);

    /**
     * This function can be used to copy, move, rename, or delete a file system object.
     *
     * @param fileOp Address of an SHFILEOPSTRUCT structure that contains information this function
     *               needs to carry out the specified operation.
     * @return Returns zero if successful, or nonzero otherwise.
     */
    int SHFileOperation(SHFILEOPSTRUCT fileOp);

    /**
     * Contains information that the SHFileOperation function uses to perform file operations.
     */
    @FieldOrder({"hwnd", "wFunc", "pFrom", "pTo", "fFlags", "fAnyOperationsAborted", "pNameMappings",
        "lpszProgressTitle"})
    class SHFILEOPSTRUCT extends Structure {
        /**
         * A window handle to the dialog box to display information about
         * the status of the file operation.
         */
        public HANDLE hwnd;
        /**
         * An FO_* value that indicates which operation to perform.
         */
        public int wFunc;
        /**
         * A pointer to one or more source file names, double null-terminated.
         */
        public String pFrom;
        /**
         * A pointer to the destination file or directory name.
         */
        public String pTo;
        /**
         * Flags that control the file operation.
         */
        public short fFlags;
        /**
         * When the function returns, this member contains TRUE if any file operations
         * were aborted before they were completed; otherwise, FALSE. An operation can
         * be manually aborted by the user through UI or it can be silently aborted by
         * the system if the FOF_NOERRORUI or FOF_NOCONFIRMATION flags were set.
         */
        public boolean fAnyOperationsAborted;
        /**
         * When the function returns, this member contains a handle to a name mapping
         * object that contains the old and new names of the renamed files. This member
         * is used only if the fFlags member includes the FOF_WANTMAPPINGHANDLE flag.
         */
        public Pointer pNameMappings;
        /**
         * A pointer to the title of a progress dialog box. This is a null-terminated string.
         */
        public String lpszProgressTitle;

        /**
         * Use this to encode <code>pFrom/pTo</code> paths.
         *
         * @param paths Paths to encode
         * @return Encoded paths
         */
        public String encodePaths(String[] paths) {
            StringBuilder encoded = new StringBuilder();
            for (String path : paths) {
                encoded.append(path);
                encoded.append("\0");
            }
            return encoded + "\0";
        }
    }

    class HANDLE extends PointerType {
        private boolean immutable;

        public HANDLE() {
        }

        public HANDLE(Pointer p) {
            setPointer(p);
            this.immutable = true;
        }

        @Override
        public void setPointer(Pointer p) {
            if (this.immutable) {
                throw new UnsupportedOperationException("immutable reference");
            }

            super.setPointer(p);
        }

        @Override
        public String toString() {
            return String.valueOf(getPointer());
        }
    }
}
