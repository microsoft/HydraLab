import colorsys
import os.path
import networkx as nx


class RouteMapVisualization:
    def __init__(self, load_path=None, display_mode=None):
        self.directed_acyclic_graph = nx.DiGraph()
        self.raw_directed_acyclic_graph = nx.DiGraph()

        self.display_mode = display_mode

        if load_path:
            self.load_route_map(load_path)

    def load_route_map(self, path):
        if os.path.exists(os.path.join(path, 'directed_acyclic_graph.gexf')):
            self.directed_acyclic_graph = nx.read_gexf(os.path.join(path, 'directed_acyclic_graph.gexf'))

        if os.path.exists(os.path.join(path, 'raw_directed_acyclic_graph.gexf')):
            self.raw_directed_acyclic_graph = nx.read_gexf(os.path.join(path, 'raw_directed_acyclic_graph.gexf'))

    def save_route_map(self, folder):
        pos = nx.fruchterman_reingold_layout(self.directed_acyclic_graph)
        max_depth = max([nx.shortest_path_length(self.directed_acyclic_graph, source='-1', target=n) for n in
                         self.directed_acyclic_graph.nodes]) - 1
        color = {}
        node_size = {}
        degrees = dict(self.directed_acyclic_graph.degree())
        for node in self.directed_acyclic_graph.nodes:
            depth = nx.shortest_path_length(self.directed_acyclic_graph, source='-1', target=node)
            if depth == 0:
                depth_percentage = 0
            else:
                depth_percentage = (depth - 1) / max_depth
            color[node] = {'r': 0, 'g': int(255 - (depth_percentage * (255 - 80))), 'b': 255, 'a': 1}
            node_size[node] = 10 + int(degrees[node] / 2) * 5
        node_attributes = {
            n: {'viz': {'position': {'x': p[0], 'y': p[1], 'z': 0}, 'size': node_size[n], 'color': color[n]}} for n, p
            in pos.items()}
        edge_attributes = {n: {'viz': {'size': 4, 'color': {'r': 0, 'g': 0, 'b': 0, 'a': 1}}} for n in
                           self.directed_acyclic_graph.edges()}
        nx.set_node_attributes(self.directed_acyclic_graph, node_attributes)
        nx.set_edge_attributes(self.directed_acyclic_graph, edge_attributes)
        nx.write_gexf(self.directed_acyclic_graph, os.path.join(folder, 'directed_acyclic_graph.gexf'))
        nx.write_gexf(self.raw_directed_acyclic_graph, os.path.join(folder, 'raw_directed_acyclic_graph.gexf'))

        tree_diagram = nx.bfs_tree(self.directed_acyclic_graph, "0")
        nx.write_gexf(tree_diagram, os.path.join(folder, 'tree_diagram.gexf'))

    def add_node(self, src_group_id, src_id, src_attr):
        if not self.directed_acyclic_graph.has_node(src_group_id):
            self.directed_acyclic_graph.add_node(src_group_id, attr=src_attr)
        self.raw_directed_acyclic_graph.add_node(src_id, attr=src_attr)

    def add_node_and_edge(self, src_group_id: str, src_id: str, dst_group_id: str, dst_id: str, dst_attr, element_attr):
        if not self.directed_acyclic_graph.has_node(dst_group_id):
            self.directed_acyclic_graph.add_node(dst_group_id, attr=dst_attr)
        if not self.directed_acyclic_graph.has_edge(src_group_id, dst_group_id):
            self.directed_acyclic_graph.add_edge(src_group_id, dst_group_id, attr=f'"{element_attr}"')

        self.raw_directed_acyclic_graph.add_node(dst_id, attr=dst_attr)
        self.raw_directed_acyclic_graph.add_edge(src_id, dst_id, attr=f'"{element_attr}"')
