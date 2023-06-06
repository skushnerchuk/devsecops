## Безопасная разработка и уязвимости программного кода

https://github.com/analysis-tools-dev/static-analysis

https://github.com/analysis-tools-dev/dynamic-analysis

```c++
// board.c
// Based on MITRE's CWE-20, demonstrative example 2
// https://cwe.mitre.org/data/definitions/20.html
#include <stdio.h>
#include <stdlib.h>

#define MAX_DIM 100

typedef struct _board_square_t {
  char color;
} board_square_t;

#define die(s)                                                                 \
  fprintf(stderr, s);                                                          \
  exit(1)

int main() {

  /* board dimensions */
  int m, n, error;
  board_square_t *board;

  printf("Please specify the board height: \n");
  error = scanf("%d", &m);
  if (EOF == error) {
    die("No integer passed: Die evil hacker!\n");
  }
  printf("Please specify the board width: \n");
  error = scanf("%d", &n);
  if (EOF == error) {
    die("No integer passed: Die evil hacker!\n");
  }
  if (m > MAX_DIM || n > MAX_DIM) {
    die("Value too large: Die evil hacker!\n");
  }
  board = (board_square_t *)malloc(m * n * sizeof(board_square_t));
  /* do some usefull staff with board */
  return 0;
}
```



```c++
// free.c
#include <stdlib.h>
#define SIZE 16
int main(){
    char* ptr = (char*)malloc (SIZE);
    if (1) {
        free(ptr);
    }
    free(ptr);
    return 0;
}
```



```c++
// html.c
// Based on MITRE's CWE-787, demonstrative example 4
// https://cwe.mitre.org/data/definitions/787.html
#include <stdlib.h>
#include <string.h>

#define MAX_SIZE 16

char * copy_input(char *user_supplied_string) {
  int i, dst_index;
  char *dst_buf = (char*)malloc(4*sizeof(char) * MAX_SIZE);
  if ( MAX_SIZE <= strlen(user_supplied_string) ){
    exit(1);
  }
  dst_index = 0;
  for ( i = 0; i < strlen(user_supplied_string); i++ ){
    if( '&' == user_supplied_string[i] ){
      dst_buf[dst_index++] = '&';
      dst_buf[dst_index++] = 'a';
      dst_buf[dst_index++] = 'm';
      dst_buf[dst_index++] = 'p';
      dst_buf[dst_index++] = ';';
    }
    else if ('<' == user_supplied_string[i] ){
      dst_buf[dst_index++] = '&';
      dst_buf[dst_index++] = 'l';
      dst_buf[dst_index++] = 't';
    }
    else dst_buf[dst_index++] = user_supplied_string[i];
  }
  return dst_buf;
}

int main() {
  char *benevolent_string = "<a href='ab&c'>";
  copy_input(benevolent_string);
  char *malicious_string  = "&&&&&&&&&&&&&&&";
  copy_input(malicious_string);
  return 0;
}
```



```c++
// packet.c
#include <stdlib.h>

int packet_get_int_ok() {
  return 123456;
}

int packet_get_int_problem() {
  return 12073741824;
}

char *packet_get_string(const char *s) {
  return "string";
}

int main() {
  char **response;
  int nresp = packet_get_int_ok();
  if (nresp > 0) {
    response = malloc(nresp*sizeof(char*));
    for (int i = 0; i < nresp; i++) response[i] = packet_get_string(NULL);
  }
  free(response);

  nresp = packet_get_int_problem();
  if (nresp > 0) {
    response = malloc(nresp*sizeof(char*));
    for (int i = 0; i < nresp; i++) response[i] = packet_get_string(NULL);
  }
  free(response);
  return 0;
}
```



```c++
// print.c
// CWE-134

#include <stdio.h>
#include <string.h>

void printWrapper(char *string) {
    printf(string);
}

int main(int argc, char **argv) {
    char buf[5012];
    memcpy(buf, argv[1], 5012);
    printWrapper(argv[1]);
    return 0;
}
```



