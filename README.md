Extension Choice Parameter plugin
=================================

Japanese version of this document is README_ja.md

Jenkins plugin to define choice build parameters where the way to provide choices can be extended.

What's this?
------------

Extension Choice Parameter plugin is a [Jenkins](http://jenkins-ci.org/) plugin.
This plugin provides Extensible Choice parameter:

* When building, the value can be selected with a dropdown like using the built-in Choice parameter.
* The choices can be provided in several ways:
	* Textarea Choice Parameter: writes choices in a textarea, just like the built-in Choice parameter.
	* Global Choice Parameter: defines choices in the Configure System page.
	* System Groovy Script Choice Parameter: runs a System Groovy script to determine the list of choices
	* File Choice Parameter: lists files in a directory.
* With Global Choice Parameter, you can deal with the case that the choices is shared by multiple jobs. Updating the choice in the Configure System will immediately affect all the jobs that uses the updated choice.
* The parameter can be set to Editable, which allows you to specify any value, even the one not in the choices. This is useful in the case that the build must be run with an irregular parameter.
	*  Edited values can be added to the choice used next time automatically.
* The way to provide choices can be extended by using [the Jenkins extention point featere] (https://wiki.jenkins-ci.org/display/JENKINS/Extension+points).

Extension point
---------------

A new way to provide choices can be added with extending `ChoiceListProvider`, overriding the following method:

```java
abstract public List<String> ChoiceListProvider::getChoiceList()
```

