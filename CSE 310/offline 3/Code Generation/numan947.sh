if bison -d -v -y parser.y; then
	echo 'bison xxx.y DONE'
	if g++ -w -c -o y.o y.tab.c; then
		echo 'g++ y.tab.c DONE'
		if flex scanner.l; then
			echo 'flex xxx.l  DONE'
			#if g++ -w -c -o l.o lex.yy.c; then
			# if the above command doesn't work try g++ -fpermissive -w -c -o l.o lex.yy.c
			if g++ -fpermissive -w -c -o l.o lex.yy.c; then
				echo 'g++ lex.yy.c DONE'
				if g++ -o a.out y.o l.o -lfl -ly; then
					echo './a.out DONE'
					./a.out input1.txt
				fi
			fi
		fi
	fi
fi