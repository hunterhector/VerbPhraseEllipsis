Repository for Verb Phrase Ellipsis annotation
-------------------------------------------------

In a recent effort we clean the data annotated by Nielsen[1]. We convert the parser dependent annotation offset to a recent format used by Bos and Spenader [2]. The standoff annotation is located at directory "annotations/".

To use the data, one also need to get raw text of the BNC data (which are originally in XML format, available [here](http://ota.ox.ac.uk/desc/2554)). We provide our code to obtain the text, in Java and Python respectively:

> [Python version](https://github.com/hunterhector/VerbPhraseEllipsis/blob/master/src/main/python/BNCReader.py)
> This is slightly slower, but very easy to use, and should be
> sufficient. Recommended.
> 
> Usage: BNCReader.py [Unpacked BNC directory] [Output Path]
> 
> [Java version](https://github.com/hunterhector/VerbPhraseEllipsis/blob/master/src/main/java/edu/cmu/cs/lti/neilson/annotation/BNCAsPlainText.java)
> Faster, but you need to run Maven to compile it. The command line
> argument should be [Unpacked BNC directory]/Texts, the output can be
> found at "data/bnc_converted".

Now locate the output directory, you will find the raw text for each BNC section. The only sections you need are the following ones, the rest are not annotated.

    A0P A19 A2U C8T CS6 EDJ EWC FNS FR3 FU6 G1A H7F HA3 J25

[1] Leif Arda Nielsen. 2005. A corpus-based study of Verb Phrase Ellipsis Identification and Resolution. Ph.D. thesis, King’s College London.

[2] Johan Bos and Jennifer Spenader. 2011. An annotated corpus for the analysis of VP ellipsis. Language Resources and Evaluation, 45(4):463–494.

License:
--------

###Code
The code in this repository is licensed under Apache V2.0, you can find a copy in this repository.

###Dataset
Our cleaned version of the standoff annotation is licensed under a [Creative Commons Attribution 4.0 International License](http://creativecommons.org/licenses/by/4.0/). The original data is developed by [Leif Arda Nielsen](https://sites.google.com/site/leifardanielsen/vpe-dataset-and-code), we thanks Leif for allowing us to republish the data again.

![Creative Commons License](https://i.creativecommons.org/l/by/4.0/88x31.png)



