# T2 DSD - Desenvolvimento de Sistemas Paralelos e Distribuídos - Simulador de Malha Viária

Aplicação em Java/JavaFX que simula o tráfego de carros em uma malha viária, utilizando Threads e implementando controle de concorrência por meio de **Semáforo** e **Monitor Java** (synchronized).

## Créditos

* **Professor:** Fernando dos Santos
* **Integrantes:** André Henrique Ludwig, Vitor André Gehrke

---

## Visão Geral

A aplicação simula carros transitando em uma malha viária, demonstrando dois mecanismos de exclusão mútua para garantir que apenas um veículo acesse regiões críticas como quadrantes da malha, ou mais especificamente, cruzamentos destas:

1. **Semáforo** (`SimulacaoControllerSemaforo` e `CarroSemaforo`)
2. **Monitor Java** (`SimulacaoControllerMonitor` e `CarroMonitor`), com blocos `synchronized` nos métodos críticos.

A interface gráfica desenvolvida em JavaFX exibe a malha e anima o fluxo de veículos.

---

## Tecnologias

* Java 21
* JavaFX 21
* Maven

---

## Estrutura do Projeto

```
server/
├── pom.xml
└── src
    ├── main
    │   ├── java/br/udesc/dsd
    │   │   ├── Main.java
    │   │   ├── controller
    │   │   │   ├── ISimulacaoController.java
    │   │   │   ├── MalhaController.java
    │   │   │   ├── SimulacaoControllerMonitor.java
    │   │   │   └── SimulacaoControllerSemaforo.java
    │   │   ├── factory/ControllerFactory.java
    │   │   ├── model
    │   │   │   ├── CarroMonitor.java
    │   │   │   ├── CarroSemaforo.java
    │   │   │   ├── Direcao.java
    │   │   │   ├── ICarro.java
    │   │   │   ├── MalhaViaria.java
    │   │   │   └── Quadrante.java
    │   │   └── view
    │   │       ├── MainFX.java
    │   │       └── MalhaView.java
    │   └── resources
    │       ├── malha-exemplo-1.txt
    │       ├── malha-exemplo-2.txt
    │       └── malha-exemplo-3.txt
    └── test/java
```

---

## Pré-requisitos

1. Java 21 instalado e `JAVA_HOME` configurado.
2. Maven (>=3.6).

---

## Como Compilar e Executar

### Execução via Maven (recomendado)

```bash
cd server
mvn clean javafx:run
```

### Gerar JAR e Executar com `--module-path`

Ajuste o caminho de acordo com sua instalação ou repositório local Maven (caso não use SDK separado). Exemplo genérico:

```bash
java \
--module-path /caminho/para/javafx/sdk/lib \
--add-modules javafx.controls \
-jar target/server-1.0-SNAPSHOT.jar
```

---

## Mecanismos de Exclusão Mútua

### Semáforo

* Implementado em `SimulacaoControllerSemaforo`, `CarroSemaforo` e na classe `Quadrante`.
* Cada `Quadrante` contém um `Semaphore`, usado para garantir que haja apenas um carro naquele quadrante por vez.
* Antes de entrar no quadrante, o carro utiliza `semaphore.tryAcquire()` para tentar ocupar o espaço; ao sair, chama `semaphore.release()`.

### Monitor Java (synchronized)

* Implementado em `SimulacaoControllerMonitor` e `CarroMonitor`.
* Blocos `synchronized` em métodos de acesso à região crítica (quadrante), utilizando o lock intrínseco de cada objeto (monitor Java). 
* Garante que apenas um carro execute a seção crítica por vez.
* Uso de `wait()` e `notifyAll()` para coordenar threads e evitar deadlocks.

---

## Arquivos de Exemplo

Localizados em `src/main/resources`:

* `malha-exemplo-1.txt`
* `malha-exemplo-2.txt`
* `malha-exemplo-3.txt`

Para carregar uma malha diferente, selecione o arquivo na interface e clique em **Iniciar Simulação**.

Também é possível incluir uma malha customizada adicionando o arquivo `.txt` em `src/main/resources/`, junto às malhas exemplo.