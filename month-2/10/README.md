## Безопасная разработка и уязвимости программного кода

Рассмотрим несколько примеров, содержащих те или иные типы уязвимостей. В таблице ниже представлен результат анализа

| Имя файла                  | Номер строки | Тип уязвимости                                               |
| -------------------------- | ------------ | ------------------------------------------------------------ |
| [board.c](./src/board.c)   | 36           | [Memory leak](https://owasp.org/www-community/vulnerabilities/Memory_leak) |
| [free.c](./src/free.c)     | 8            | [Doubly freeing memory](https://owasp.org/www-community/vulnerabilities/Doubly_freeing_memory) |
| [html.c](./src/html.c)     | 22           | [Buffer Overflow](https://owasp.org/www-community/vulnerabilities/Buffer_Overflow) |
| [packet.c](./src/packet.c) | 38           | [Doubly freeing memory](https://owasp.org/www-community/vulnerabilities/Doubly_freeing_memory) |
| [print.c](./src/print.c)   | 12           | [Buffer Overflow](https://owasp.org/www-community/vulnerabilities/Buffer_Overflow) |

Ниже представлены отчеты статического и динамического анализа по всем примерам:

#### [board.c](./src/board.c)

**clang-tidy report**

```
/build/app/board.c:36:3: warning: Value stored to 'board' is never read [clang-analyzer-deadcode.DeadStores]
  board = (board_square_t *)malloc(m * n * sizeof(board_square_t));
  ^
/build/app/board.c:36:3: note: Value stored to 'board' is never read
/build/app/board.c:38:3: warning: Potential leak of memory pointed to by 'board' [clang-analyzer-unix.Malloc]
  return 0;
  ^
/build/app/board.c:25:3: note: Taking false branch
  if (EOF == error) {
  ^
/build/app/board.c:30:3: note: Taking false branch
  if (EOF == error) {
  ^
/build/app/board.c:33:7: note: Assuming 'm' is <= MAX_DIM
  if (m > MAX_DIM || n > MAX_DIM) {
      ^
/build/app/board.c:33:7: note: Left side of '||' is false
/build/app/board.c:33:22: note: Assuming 'n' is <= MAX_DIM
  if (m > MAX_DIM || n > MAX_DIM) {
                     ^
/build/app/board.c:33:3: note: Taking false branch
  if (m > MAX_DIM || n > MAX_DIM) {
  ^
/build/app/board.c:36:29: note: Memory is allocated
  board = (board_square_t *)malloc(m * n * sizeof(board_square_t));
                            ^
/build/app/board.c:38:3: note: Potential leak of memory pointed to by 'board'
  return 0;
  ^
```

**Address sanitizer:**

```
Please specify the board height: 
10
Please specify the board width: 
10

=================================================================
==75300==ERROR: LeakSanitizer: detected memory leaks

Direct leak of 100 byte(s) in 1 object(s) allocated from:
    #0 0x7f3832cdecaf in __interceptor_malloc ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:69
    #1 0x55ebcf9be5e3 in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/board.c:36
    #2 0x7f3832823a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58

SUMMARY: AddressSanitizer: 100 byte(s) leaked in 1 allocation(s).
```

#### [free.c](./src/free.c)

**clang-tidy report**

```
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free.c:8:5: warning: Attempt to free released memory [clang-analyzer-unix.Malloc]
    free(ptr);
    ^~~~~~~~~
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free.c:4:24: note: Memory is allocated
    char* ptr = (char*)malloc (SIZE);
                       ^~~~~~~~~~~~~
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free.c:5:5: note: Taking true branch
    if (1) {
    ^
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free.c:6:9: note: Memory is released
        free(ptr);
        ^~~~~~~~~
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free.c:8:5: note: Attempt to free released memory
    free(ptr);
```

**Address sanitizer:**

```
=================================================================
==76390==ERROR: AddressSanitizer: attempting double-free on 0x602000000010 in thread T0:
    #0 0x7f399c0dd7f0 in __interceptor_free ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:52
    #1 0x55866fa8e1da in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free.c:8
    #2 0x7f399bc23a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58
    #3 0x7f399bc23b48 in __libc_start_main_impl ../csu/libc-start.c:360
    #4 0x55866fa8e0e4 in _start (/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free+0x10e4) (BuildId: 4efac1f8fbab81721aaf30672a8df78fd0aee786)

0x602000000010 is located 0 bytes inside of 16-byte region [0x602000000010,0x602000000020)
freed by thread T0 here:
    #0 0x7f399c0dd7f0 in __interceptor_free ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:52
    #1 0x55866fa8e1ce in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free.c:6
    #2 0x7f399bc23a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58

previously allocated by thread T0 here:
    #0 0x7f399c0decaf in __interceptor_malloc ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:69
    #1 0x55866fa8e1be in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/free.c:4
    #2 0x7f399bc23a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58

SUMMARY: AddressSanitizer: double-free ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:52 in __interceptor_free
==76390==ABORTING
```

#### [html.c](./src/html.c)

**clang-tidy report**

```
Нет
```

**Address sanitizer:**

```
=================================================================
==77017==ERROR: AddressSanitizer: heap-buffer-overflow on address 0x6060000000c0 at pc 0x560a0611147d bp 0x7ffd021eb940 sp 0x7ffd021eb930
WRITE of size 1 at 0x6060000000c0 thread T0
    #0 0x560a0611147c in copy_input /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/html.c:22
    #1 0x560a061116ac in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/html.c:38
    #2 0x7f3058a23a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58
    #3 0x7f3058a23b48 in __libc_start_main_impl ../csu/libc-start.c:360
    #4 0x560a061111a4 in _start (/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/html+0x11a4) (BuildId: 02f3de687da42b0ca84cda4b31f5f504148481c5)

0x6060000000c0 is located 0 bytes after 64-byte region [0x606000000080,0x6060000000c0)
allocated by thread T0 here:
    #0 0x7f3058edecaf in __interceptor_malloc ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:69
    #1 0x560a06111283 in copy_input /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/html.c:11
    #2 0x560a061116ac in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/html.c:38
    #3 0x7f3058a23a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58

SUMMARY: AddressSanitizer: heap-buffer-overflow /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/html.c:22 in copy_input
Shadow bytes around the buggy address:
  0x605ffffffe00: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  0x605ffffffe80: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  0x605fffffff00: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  0x605fffffff80: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
  0x606000000000: fa fa fa fa 00 00 00 00 00 00 00 00 fa fa fa fa
=>0x606000000080: 00 00 00 00 00 00 00 00[fa]fa fa fa fa fa fa fa
  0x606000000100: fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa
  0x606000000180: fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa
  0x606000000200: fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa
  0x606000000280: fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa
  0x606000000300: fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa fa
Shadow byte legend (one shadow byte represents 8 application bytes):
  Addressable:           00
  Partially addressable: 01 02 03 04 05 06 07 
  Heap left redzone:       fa
  Freed heap region:       fd
  Stack left redzone:      f1
  Stack mid redzone:       f2
  Stack right redzone:     f3
  Stack after return:      f5
  Stack use after scope:   f8
  Global redzone:          f9
  Global init order:       f6
  Poisoned by user:        f7
  Container overflow:      fc
  Array cookie:            ac
  Intra object redzone:    bb
  ASan internal:           fe
  Left alloca redzone:     ca
  Right alloca redzone:    cb
==77017==ABORTING
```

#### [packet.c](./src/packet.c)

**clang-tidy report**

```
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/packet.c:8:10: warning: implicit conversion from 'long' to 'int' changes value from 12073741824 to -811160064 [clang-diagnostic-constant-conversion]
  return 12073741824;
  ~~~~~~ ^~~~~~~~~~~
```

**Address sanitizer:**

```
=================================================================
==77633==ERROR: AddressSanitizer: attempting double-free on 0x7fa3a900d800 in thread T0:
    #0 0x7fa3a9add7f0 in __interceptor_free ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:52
    #1 0x55730810735e in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/packet.c:2299
    #2 0x7fa3a9623a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58
    #3 0x7fa3a9623b48 in __libc_start_main_impl ../csu/libc-start.c:360
    #4 0x557308107144 in _start (/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/packet+0x1144) (BuildId: f7e00ea2a8bdc0f08b642a50a1633912a684a903)

0x7fa3a900d800 is located 0 bytes inside of 987648-byte region [0x7fa3a900d800,0x7fa3a90fea00)
freed by thread T0 here:
    #0 0x7fa3a9add7f0 in __interceptor_free ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:52
    #1 0x5573081072d3 in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/packet.c:22
    #2 0x7fa3a9623a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58

previously allocated by thread T0 here:
    #0 0x7fa3a9adecaf in __interceptor_malloc ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:69
    #1 0x55730810726c in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/packet.c:19
    #2 0x7fa3a9623a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58

SUMMARY: AddressSanitizer: double-free ../../../../src/libsanitizer/asan/asan_malloc_linux.cpp:52 in __interceptor_free
==77633==ABORTING
```

#### [print.c](./src/print.c)

**clang-tidy report**

```
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/print.c:7:12: warning: format string is not a string literal (potentially insecure) [clang-diagnostic-format-security]
    printf(string);
           ^~~~~~
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/print.c:7:12: note: treat the string as an argument to avoid this
    printf(string);
           ^
           "%s", 
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/print.c:12:5: warning: Call to function 'memcpy' is insecure as it does not provide security checks introduced in the C11 standard. Replace with analogous functions that support length arguments or provides boundary checks such as 'memcpy_s' in case of C11 [clang-analyzer-security.insecureAPI.DeprecatedOrUnsafeBufferHandling]
    memcpy(buf, argv[1], 5012);
    ^~~~~~
/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/print.c:12:5: note: Call to function 'memcpy' is insecure as it does not provide security checks introduced in the C11 standard. Replace with analogous functions that support length arguments or provides boundary checks such as 'memcpy_s' in case of C11
    memcpy(buf, argv[1], 5012);
    ^~~~~~
```

**Address sanitizer:**

```
AddressSanitizer:DEADLYSIGNAL
=================================================================
==78460==ERROR: AddressSanitizer: SEGV on unknown address 0x000000000000 (pc 0x55960797b485 bp 0x7fffa520b950 sp 0x7fffa520a440 T0)
==78460==The signal is caused by a READ memory access.
==78460==Hint: address points to the zero page.
    #0 0x55960797b485 in main /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/print.c:12
    #1 0x7f6b1de23a8f in __libc_start_call_main ../sysdeps/nptl/libc_start_call_main.h:58
    #2 0x7f6b1de23b48 in __libc_start_main_impl ../csu/libc-start.c:360
    #3 0x55960797b184 in _start (/media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/print+0x1184) (BuildId: 13bd5f83e73feb26228d597e5a31cc3bdfe2903e)

AddressSanitizer can not provide additional info.
SUMMARY: AddressSanitizer: SEGV /media/sergey/data/dev/otus/DevSecOps-2023-03/month-2/10/src/print.c:12 in main
==78460==ABORTING
```

