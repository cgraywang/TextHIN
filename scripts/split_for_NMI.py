import os
import sys

def adjust(i, sim):
	parts = sim.split(" ")
	pre_self_sim = float(parts[i])
	self_sim = max(pre_self_sim, 2.0)
	parts[i] = str(self_sim)
	if pre_self_sim < self_sim:
		print i, pre_self_sim
	return " ".join(parts)


sim_file = sys.argv[1]
out_sim_file = sys.argv[2]
out_label_file = sys.argv[3]
domains = []

out_sim = file(out_sim_file, 'w')
out_label = file(out_label_file, 'w')
for idx, line in enumerate(file(sim_file, 'r')):
	doc, sim = line.strip().split("\t")
	sim = adjust(idx, sim)
	domain = doc.split(" ||| ")[1]
	if not domain in domains:
		domains.append(domain)
	out_sim.write(sim)
	out_sim.write("\n")
	label = domains.index(domain)
	out_label.write(str(label) + "\n")
print domains
print "classnum =", len(domains)
out_sim.close()
out_label.close()
