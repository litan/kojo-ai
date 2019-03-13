# Kojo-AI
Support for data science, machine learning, and more - within Kojo

### Current Features (work in progress, but functional):
* Data Frames (via Tablesaw)
* Plotting (via XCharts)
* Neural Networks (via Tensorflow)

Kojo-AI aims to provide an integrated high-level API/DSL sitting over the (lower level) libraries mentioned above.

### Upcoming Features:
* Search
* Constraint Satisfaction
* Constraint Logic Programming
* Genetic Algorithms

To use Kojo-AI within Kojo, copy artifacts from this repo into the Kojo libk directory:  
`sbt buildDist`  
`mv ~/.kojo/lite/libk ~/.kojo/lite/libk.bak`  
`cp -var dist ~/.kojo/lite/libk`

Then (re)start Kojo to start using Kojo-AI.

A blog post with examples will be available soon. In the meantime here are some (raw) examples:  
https://github.com/litan/kojo-ai/tree/master/src/main/scala/example
