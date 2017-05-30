# /usr/bin/python

import sys
import os
import string

delimiters = set(string.punctuation)

if len(sys.argv) < 3:
    print("[Usage] Validator [annotation directory] [text directory]")
    exit(1)
anno_dir = sys.argv[1]
text_dir = sys.argv[2]


def check_delimiter(char):
    if not char.isspace() or not char in delimiters:
        return False
    return True


def check_space(text, begin, end):
    if begin > 0:
        if not check_delimiter(text[begin - 1]):
            return False
    if end < len(text) - 1:
        if not check_delimiter(text[end]):
            return False
    return True

errors = 0
for anno in os.listdir(anno_dir):
    base = anno.split(".")[0]

    with open(os.path.join(anno_dir, anno)) as a, open(os.path.join(text_dir, base + ".txt")) as t:
        print("=====In document: %s=====" % base)

        # Important, must decode with UTF-8 to make the offsets correct.
        text = t.read().decode("utf-8")
        for line in a:
            parts = line.split()
            target_begin, target_end, source_begin, source_end = \
                int(parts[1]), int(parts[2]), int(parts[3]), int(parts[4])

            if check_space(text, source_begin, source_end):
                print("Source error at document %s, source is [%d:%d]:%s, not surrounded by delimiter." %
                      (base, source_begin, source_end, text[source_begin: source_end]))
                errors += 1

            if check_space(text, target_begin, target_end):
                print("Target error at document %s, target is [%d:%d]:%s, not surrounded by delimiter." %
                      (base, target_begin, target_end, text[target_begin: target_end]))
                errors += 1
                print("%s -> %s" % (text[target_begin: target_end], text[source_begin: source_end]))

print("%d errors found." % errors)
