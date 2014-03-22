#!/usr/bin/python
'''
Screenscraper die productnamen van de Nederlandse IKEA-website verzamelt.
De namen bevinden zich per beginletter op een andere pagina op deze plekken:
... <span class="productsAzLink"><a href="...">hier</a></span> ...
en zien er in het meest complexe geval zo uit:
BILLY/BILLY VALBO boekenkast met glazen deur
Dit wordt gesplitst in "BILLY" en "BILLY VALBO".
Resultaten worden ontdubbeld, gesorteerd en per regel opgeslagen in
opslaan_filename. Runnen vanuit de directory waar dit script staat.
'''
import urllib2
import HTMLParser
import itertools
import codecs

opslaan_filename = "../src/main/java/nl/topicus/plugins/maven/i2a/names.txt"

class Extractor(HTMLParser.HTMLParser):
    def __init__(self):
        HTMLParser.HTMLParser.__init__(self)
        self.stack = None
        self.extracted = []
    def handle_starttag(self,tag,attrs):
        if tag=='span' and ('class','productsAzLink') in attrs:
            self.stack = tag
    def handle_endtag(self,tag):
        self.stack = None
    def handle_data(self,data):
        if self.stack:
            self.extracted.append(data.decode('utf-8'))

def scrape(nr):
    f = urllib2.urlopen("http://www.ikea.com/nl/nl/catalog/productsaz/{0}/".format(nr))
    parser = Extractor()
    parser.feed(f.read())
    f.close()
    return parser.extracted

def take_upper_words(phrase):
    return ' '.join(itertools.takewhile(unicode.isupper,phrase.split()))

def main():
    names = []
    for nr in range(26):
        phrases = scrape(nr)
        names.extend(take_upper_words(sp) for p in phrases for sp in p.split('/'))
    dedup = set(names)
    dedup.remove('')
    sortednames = list(dedup)
    sortednames.sort()
    with codecs.open(opslaan_filename, encoding='utf-8', mode='w') as f:
        f.write('\n'.join(sortednames) + '\n')

if __name__=='__main__':
    main()
