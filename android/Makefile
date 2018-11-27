all : deploy

clean : 
	bash gradlew :sdk:clean

deploy : install upload

install :
	bash gradlew :sdk:install

upload :
	bash gradlew :sdk:bintrayUpload
