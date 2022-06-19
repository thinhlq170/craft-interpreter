CC=gcc

all: main

main: main.o debug.o memory.o chunk.o value.o
	gcc -Wall -o main main.o debug.o memory.o chunk.o value.o

debug.o: debug.c
	gcc -c -Wall debug.c

memory.o: memory.c
	gcc -c -Wall memory.c

chunk.o: chunk.c
	gcc -c -Wall chunk.c

value.o: value.c
	gcc -c -Wall value.c