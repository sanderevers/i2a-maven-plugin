#!/usr/bin/python

import codecs

words = set()
with codecs.open('arabisch.txt', encoding='utf-8') as f:
    for line in f.readlines():
        for word in line.split():
            words.add(word)

with codecs.open('dictionary_ar.txt', encoding='utf-8', mode='w') as f:
    f.write('\n'.join(words))
