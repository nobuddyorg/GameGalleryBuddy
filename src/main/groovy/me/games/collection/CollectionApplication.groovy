package me.games.collection


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class CollectionApplication {

    static void main(String[] args) {
        SpringApplication.run(CollectionApplication, args)
        println '\n\033[0;34mopen: http://localhost:8080/collection?username=besessener&size=85&showName=no&showUrl=no&shuffle=yes\033[0m'
    }
}
