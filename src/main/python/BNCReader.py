#!/usr/bin/python

"""
The python code that convert the BNC text into raw text, which correspond to the annotations.
"""

import os
import xml.etree.ElementTree as ET

import sys


def get_sentence_words(f):
    root = ET.parse(f)
    sentences = []
    for s_index, s_node in enumerate(root.findall('.//s')):
        l = [text.encode('utf-8') for text in s_node.itertext()]
        sentences.append(l)
    return sentences


bnc_source_dir = sys.argv[1] + "/Texts"  # e.g. bnc download and unpack directory
output_path = sys.argv[2]

bnc_plain_text_dir = os.path.join(output_path, "bnc_raw")
if not os.path.exists(bnc_plain_text_dir):
    os.makedirs(bnc_plain_text_dir)

file_count = 0
for root, dirs, files in os.walk(bnc_source_dir):
    for name in files:
        if not name.endswith(".xml"):
            continue
        sys.stdout.write("\rWorking on : %s (%d finished)." % (name, file_count))
        sys.stdout.flush()
        file_count += 1
        source_path = os.path.join(root, name)
        output_name = os.path.splitext(name)[0] + ".txt"
        plain_text_path = os.path.join(bnc_plain_text_dir, output_name)

        with open(source_path) as bnc_source, open(plain_text_path, 'w') as plain_text_out:
            for s_words in get_sentence_words(bnc_source):
                plain_text_out.write(''.join(s_words))
                plain_text_out.write("\n")
        plain_text_out.close()
