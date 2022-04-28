package reactor

import explorer.ExplorerHandler
import client.ChainClient

/**
 * Fusion Lab
 *
 * Where atomic fusions are built. In the lab, the parts
 * that is required to build the atomic fusion are gathered
 * and used to model an Atomic Fusion.
 *
 * For example, the Fusion Lab will
 * 1. reach out to the explorer and retrieve the required input boxes.
 * 2. Get data from required db to model the outputs
 * 3. The output will be an atomic fusion that is ready to be sent
 * to the chamber
 */
abstract class FusionLab(explorer: ExplorerHandler, client: ChainClient) {

}
