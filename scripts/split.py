import sys
import os

class FeatureVector:
	max_self = 0
	def __init__(self, index, label, feature, feature_idx, feature_map):
		self.index = feature_map[index] + 1
		arr = line.split(" ")
		self.feature = self.extract(feature.split(" "), feature_idx)
		self.label = label

	def extract(self, feature, extract_idxs):
		sub_feature = []
		sub_idx = 0
		for idx in extract_idxs:
			value = feature_idx[idx]
			sub_feature.append(str(sub_idx+1) + ":" + feature[idx])
			sub_idx += 1
		return sub_feature

	def kernel_str(self):
		l = []
		l.append(str(self.label))
		l.append("0:" + str(self.index))
		l.extend(self.feature)
		return " ".join(l)
	

sim_file = sys.argv[1]
domain1 = sys.argv[2]
domain2 = sys.argv[3]
out_dir = sys.argv[4]
feature_map = {}
domains = [domain1, domain2]
domain_docs = {}
for domain in domains:
	domain_docs[domain] = []

for idx, line in enumerate(file(sim_file, 'r')):
	domain, feature = line.strip().split("\t")
	domain = domain.split(" ||| ")[1]
	if domain in domain_docs:
		domain_docs[domain].append(idx)
		feature_map[idx] = len(feature_map)
for domain in domain_docs:
	print domain, len(domain_docs[domain])


ratio = 0.8
docs = set()
train_docs = set()
test_docs = set()
for domain in domain_docs:
	size = len(domain_docs[domain])
	splitter = int(size * ratio)
	train_docs.update(domain_docs[domain][:splitter])
	test_docs.update(domain_docs[domain][splitter:])
	docs.update(domain_docs[domain])
feature_idx = sorted(docs)

if not os.path.exists(out_dir):
	os.makedirs(out_dir)
train_out = file(os.path.join((out_dir), "train.svm"), 'w')
test_out = file(os.path.join((out_dir), "test.svm"), 'w')

for idx,line in enumerate(file(sim_file, "r")):
	if idx in docs:
		label, feature = line.strip().split("\t")
		label = label.split(" ||| ")[1]
		label = domains.index(label)
		fv = FeatureVector(idx, label, feature, feature_idx, feature_map)
		if idx in train_docs:
			train_out.write(fv.kernel_str())
			train_out.write("\n")
		if idx in test_docs:
			test_out.write(fv.kernel_str())
			test_out.write("\n")
train_out.close()
test_out.close()
