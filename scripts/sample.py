inf = file('../data/20NG/20news.v2.txt')

docs = set()
domain_rest = {}
doc_outf = file('../test/testdoc.txt', 'w')
for line in inf:
	if " ||| " in line:
		docid = line.strip()
	else:
		domain = docid.strip().split(" ||| ")[1]
		if not domain in domain_rest:
			domain_rest[domain] = 5
		if domain_rest[domain] == 0: 
			continue
		else:
			domain_rest[domain] -= 1
			docs.add(docid)
			doc_outf.write(docid + "\n" + line)
doc_outf.close()
inf.close()

linking_outf = file("../test/testdoc.linking.txt", 'w')
for line in file("../test/20news.entity.chunk15.nlp3.6.txt"):
	if " ||| " in line:
		docid = line.strip()
		if docid in docs:
			linking_outf.write(line)
	else:
		if docid in docs:
			linking_outf.write(line)
linking_outf.close()


