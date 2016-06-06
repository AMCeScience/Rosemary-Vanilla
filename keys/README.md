# Encryption and Decryption Keys

You need to generate a pair of public and private keys. These keys are used to encrypt sensitive information such as passwords before storing them in the MongoDB and decrypt them before use.

## How to Generate Keys

* Install [Maven 3](https://maven.apache.org/install.html).

```
sudo port install maven3
sudo port select --set maven maven3
```

* Clone [Keyczar](https://github.com/google/keyczar) and change directory to `keyczar/java/code`.

```
git clone git@github.com:google/keyczar.git
cd keyczar/java/code
```

* Build the `keyczar` project, in particular the executable `jar` file.

```
mvn package -DskipTests
cd target
ls KeyczarTool*
```

* Use the generated `KeyczarTool` to generate keys, remember to adjust the name of the `jar` file.

```
mkdir keys
java -jar KeyczarTool-0.71h-051816.jar create --location=keys --purpose=crypt --name=rosemary --asymmetric=rsa
java -jar KeyczarTool-0.71h-051816.jar addkey --location=keys --status=primary
```

* Copy the content of the `keys` directory to this directory. That should include two files called `meta` and `1`.
