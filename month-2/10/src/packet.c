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
