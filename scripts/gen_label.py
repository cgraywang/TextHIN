import sys
domain_label = {}
for line in sys.stdin:
	if "|||" in line:
		doc_id = line.strip()
		domain = doc_id.split(" ||| ")[1]
		if not domain in domain_label:
			domain_label[domain] = len(domain_label)
		label = domain_label[domain]
		print doc_id + "\t" + str(label)

