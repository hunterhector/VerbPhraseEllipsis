#!/usr/bin/python

"""
Convert the Brat format ellipsis annotations into Bos and Spenader's format.
"""

import glob, sys, os

if len(sys.argv) < 3:
    print("[Usage:] Brat2Bos.py [brat annotation directory] [output directory]")

out_dir = sys.argv[2]

if not os.path.exists(out_dir):
    os.makedirs(out_dir)

for brat_ann in glob.glob(sys.argv[1]+"/*.ann"):
    brat_txt = brat_ann.replace(".ann", ".txt")
    basename = os.path.basename(brat_ann)
    docid = basename.replace(".ann", "")

    raw_text = open(brat_txt).read().decode("utf-8")

    targets = {}
    sources = {}
    target_2_source = {}

    has_anno = False

    for line in open(brat_ann):
        parts = line.strip().split(None, 4)
        type = parts[1]
        id = parts[0]

        if type == "Target":
            targets[id] = (int(parts[2]), int(parts[3]), parts[4])
        elif type == "Source":
            sources[id] = (int(parts[2]), int(parts[3]))
        elif type == "VPE":
            arg1 = parts[2].split(":")[1]
            arg2 = parts[3].split(":")[1]
            target_2_source[arg1] = arg2
        has_anno = True

    if not has_anno:
        continue

    out = open(os.path.join(out_dir, basename), 'w')

    for tid, target in targets.iteritems():
        if tid in target_2_source:
            source = sources[target_2_source[tid]]

        sample = raw_text[target[0] - 30: target[1] + 30].replace("\n"," ")

        anno = "%s %d %d %d %d %s - - - ...%s...\n" % (docid, target[0], target[1], source[0], source[1], target[2], sample)
        out.write(anno.encode("utf-8"))

    out.close()
