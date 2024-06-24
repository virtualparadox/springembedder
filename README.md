# SpringEmbedder

SpringEmbedder is a Java-based application that uses the Fruchterman-Reingold algorithm to layout graphs. It leverages the JGraphT library for graph representation and the JCodec library for rendering the layouts to video.

## Project Structure

### Main Class

The entry point of the application is the `Main` class, located in the `eu.virtualparadox.springembedder` package. This class initializes the graph, sets up the layout algorithm, and starts the layout process.

### Graph Initialization

The graph is initialized using the `DemoGraphInitializer` class. This class creates a sample graph with a specified number of centers and nodes per center. It connects the nodes and assigns weights to the edges.

### Fruchterman-Reingold Algorithm

The Fruchterman-Reingold algorithm is implemented in the `FruchtermanReingoldLayouter` class. This class handles the layout process by iteratively adjusting the positions of the nodes based on simulated forces.

## Fruchterman-Reingold Algorithm

The Fruchterman-Reingold algorithm is a force-directed layout algorithm for visualizing graphs. The algorithm simulates forces acting on the nodes of the graph and iteratively adjusts their positions to minimize the overall energy of the system.

### Repulsive Force

The repulsive force $`F_r`$ between two nodes $`u`$ and $`v`$ is calculated using the formula:

$` F_r(d) = \frac{k^2}{d}`$

where \(d\) is the distance between the nodes, and $`k`$ is a constant related to the optimal distance between nodes. In the code, the calculation of repulsive forces is implemented in the `calculateRepulsiveForces` method of the `FruchtermanReingoldLayouter` class.

### Attractive Force

The attractive force $`F_a`$ between two connected nodes $`u`$ and $`v`$ is given by:

$` F_a(d) = \frac{d^2}{k} `$

This is implemented in the `calculateAttractiveForces` method of the `FruchtermanReingoldLayouter` class.

### Temperature

The temperature is used to limit the displacement of nodes and is gradually decreased in each iteration. The update of positions based on computed forces and temperature is done in the `updatePositions` method of the `FruchtermanReingoldLayouter` class.

### Edge Weight Normalization

Edge weights are normalized to ensure consistency in force calculations. This is handled by the `EdgeWeightNormalizer` class, which normalizes the weights to a specified range.

### Rendering

The rendering of the graph layout is managed by the `AbstractRendererCallback` class and its subclasses. The `PngRendererCallback` class renders each iteration to a PNG image, while the `VideoRendererCallback` class encodes the frames into a video file.

## Usage

To use this project, ensure that the required dependencies are included in your `pom.xml` file. Run the `Main` class to generate and render the graph layout.

