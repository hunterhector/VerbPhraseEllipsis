### Repository for some small VerbPhraseEllipsis Code###

In a recent effort we clean the data annotated by Nielsen[1]. We convert the parser dependent annotation offset to a recent format used by Bos and Spenader [2]. The data will come soon (or you could contact me if you need them now).

To use the data, one also need to get raw text of the BNC data (which are originally in XML format). We provide our code that obtain the annotation data we used, in Java and Python respectively.

[Python version, slower](https://github.com/hunterhector/VerbPhraseEllipsis/blob/master/src/main/python/BNCReader.py)

[Java version, you could run Maven to compile it](https://github.com/hunterhector/VerbPhraseEllipsis/blob/master/src/main/java/edu/cmu/cs/lti/neilson/annotation/BNCAsPlainText.java)


[1] Leif Arda Nielsen. 2005. A corpus-based study of Verb Phrase Ellipsis Identification and Resolution. Ph.D. thesis, King’s College London.
[2] Johan Bos and Jennifer Spenader. 2011. An annotated corpus for the analysis of VP ellipsis. Language Resources and Evaluation, 45(4):463–494.