CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lboost_thread -pthread

all: bin/BGRSclient

bin/BGRSclient: bin/connectionHandler.o bin/BGRSClient.o
	@echo 'Building target: BGRSclient'
	@echo 'Invoking: C++ Linker'
	g++ -Wall -Weffc++ -o bin/BGRSclient bin/BGRSClient.o bin/connectionHandler.o $(LDFLAGS)
	@echo 'Finished building target: bin/BGRSclient'
	@echo ' '
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) $(LDFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/BGRSClient.o: src/BGRSClient.cpp
	g++ $(CFLAGS) -o bin/BGRSClient.o src/BGRSClient.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
