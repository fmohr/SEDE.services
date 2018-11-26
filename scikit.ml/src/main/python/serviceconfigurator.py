# this is a simple script which helps configure class configuration.
import json


def configureWekaClassifier(classconfig) :
    for cls in classconfig:
        cls: str
        if cls.startswith("weka.classifiers"):
            classconfig[cls] = {
                "extends": ["$Basic_Weka_Index_Classifier$"],
                "methods": {
                    "$construct": {
                        "params": [
                            {
                                "fixed": "\"" + cls + "\"",
                                "type": "String"
                            }
                        ]
                    }
                }
            }

def configureSKClassifier(classconfig) :
    for cls in classconfig:
        cls: str
        if cls.startswith("sklearn"):
            print("\"" + cls + "\",")
            classconfig[cls]["methods"] = {
                    "$construct": {
                        "params": [
                            {
                                "fixed": "\"" + cls + "\"",
                                "type": "String"
                            }
                        ]
                    }

            }


with open("../resources/config/sl-ml-classifiers-classconf.json", "r") as fp:
    classconfig = json.load(fp)

# configureWekaClassifier(classconfig)

configureSKClassifier(classconfig)

# with open("../resources/config/ml-classifiers-classconf.json", "w") as fp:
#     json.dump(classconfig, fp, indent=4)