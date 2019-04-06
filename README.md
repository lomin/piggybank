# accounting-piggybank

## What is the problem?
Implementing distributed systems correctly is difficult.
## Why is that a problem?
Distributed systems are becoming increasingly popular due to microservices, serverless architectures, or specific migration techniques. Errors in distributed systems are particularly difficult to identify and can lead to data loss.
## What is the solution?
A lightweight modeling of the distributed system in combination with a model checker.
## Why is that the solution?
The modeling process provides clarity about sources of error. The model checker finds design errors automated, early and at relatively low cost.
## How does this project contribute to the solution?
This projects describes a very simple way to model and check the design of a distributed system. This project is neither as optimized, nor as feature-rich as alternative solutions like TLA+. The main purpose is to motivate the modeling of critical system design decision. The assumption is that a familiar syntax and programming model (especially for Clojure developers) makes it easy to learn about and to practice modelling of distributed systems.

## License

Copyright © 2019 Steven Collins

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
