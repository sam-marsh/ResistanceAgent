# Techniques

## [Bayes theorem](https://en.wikipedia.org/wiki/Bayes%27_theorem)

Describes the probability of an event, based on conditions that may be related to the event. P(A | B) = P(B | A) P(A) / P(B) i.e. the probability of A given B is the probability of B given A times the probability of A divided by the probability of B. Note in particular the Bayesian interpretation on the Wikipedia page - this translates it into belief. Will definitely need to use this to update our beliefs as the game goes on.

Useful resources:


## [Markov models](https://en.wikipedia.org/wiki/Markov_model)

 Models randomly changing ('stochastic') systems where it is assumed that future states depend only on the current state, not on the events that occured before it (the Markov property). Is this true for The Resistance?
 
 There are different types of Markov model depending on the system: fully observable/partially observable and autonomous/controlled. I think our system is partially observable (since don't have full information) and controlled (we have at least some control over the system?).
 
 I think we might need a 'partially observable Markov decision process' (POMDP). This applies where the agent cannot directly observe the underlying state. But this still assumes the system evolves randomly...
 
 ## [Monte Carlo tree search](https://en.wikipedia.org/wiki/Monte_Carlo_tree_search)
 
 A heuristic search algorithm for some kinds of decision processes (usually for game play). Used in Go and poker. Analyses most promising moves, expanding search tree using a random sample. 

## [Epistemic logic](http://plato.stanford.edu/entries/logic-epistemic/)

This is the logic of knowledge and belief (i.e. who is a spy, or possibly who is suspicious of us if we're a spy?). This type of logic describes how to store beliefs symbolically, how to deduce further beliefs from the stored beliefs, and what the beliefs actually mean (semantics).

Compared to 'extensional logics' (propositional/predicate), it changes from 'X is Y' --> 'A knows Y'
Useful resources:
- [Epistemic Logic and its Applications](https://cs.nyu.edu/davise/knowledge-tutorial.pdf)

## [Logic programming](https://en.wikipedia.org/wiki/Logic_programming)

Basically programming using formal logic. We could probably get some Prolog-interface in Java. Not sure if this will work since we need beliefs as a percentage, not 'known/not-known'.

## [Theorem proving](https://en.wikipedia.org/wiki/Automated_theorem_proving)

This might help us in finding out further facts given the knowledge we have.

## [Genetic algorithms](https://en.wikipedia.org/wiki/Genetic_algorithm)

Natural selection, as per lecture notes. This would be used if we did something like a neural network, where the network evolved as it played itself/us.

## [Neural networks](https://en.wikipedia.org/wiki/Artificial_neural_network)

Machine learning - play lots of games, if it loses, improve itself.

## [Constraint programming](https://en.wikipedia.org/wiki/Constraint_programming) 

Relations between variables stated as constraints. For example, we could say 'A or B is a spy' 'B or C is a spy' 'there is 1 spy' and it would deduce that B is a spy.
