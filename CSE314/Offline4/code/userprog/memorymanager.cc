#include "memorymanager.h"
#include <stdio.h>
#include <stdlib.h>


MemoryManager::MemoryManager(int numPages)
{
	bitMap = new BitMap(numPages);

	printf("%d NumClear\n",bitMap->NumClear() );

	processMap = new int[numPages];
	entries = new TranslationEntry[numPages];
	iterator=0;

	this->numPages = numPages;
	

	lock = new Lock("lock of memory manager");
}

MemoryManager::~MemoryManager()
{
	delete bitMap;
	delete lock;
}

int
MemoryManager::AllocPage()
{
	lock->Acquire();
	int ret = bitMap->Find();
	lock->Release();
	return ret;
}

void
MemoryManager::FreePage(int physPageNum)
{
	lock->Acquire();
	bitMap->Clear(physPageNum);
	lock->Release();
}

bool
MemoryManager::PageIsAllocated(int physPageNum)
{
	lock->Acquire();
	bool ret = bitMap->Test(physPageNum);
	lock->Release();
	return ret;
}

bool
MemoryManager::IsAnyPageFree()
{
	lock->Acquire();
	bool ret;
	if(bitMap->NumClear() == 0)
		ret = false;
	else
		ret = true;
	lock->Release();
	return ret;
}

int
MemoryManager::NumFreePages()
{
	lock->Acquire();
	int ret = bitMap->NumClear();
	lock->Release();
	return ret;
}


int MemoryManager::AllocPage(int processNo, TranslationEntry &entry)
{
	lock->Acquire();
	
	int ret = bitMap->Find();

	if(ret!=-1){ //valid
		processMap[ret] = processNo;
		entries[ret] = entry;
	}
	
	lock->Release();

	return ret;
}


int MemoryManager::AllocByForce()
{
	return rand()%numPages; //returning random number
}

TranslationEntry& MemoryManager::getTranslationEntry(int physPageNum)
{
	return entries[physPageNum];
}

int& MemoryManager::getProcessId(int physPageNum)
{
	return processMap[physPageNum];
}