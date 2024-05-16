from common.action_definitions import ActionPlan, ExploreInstructions, PageContext
import common.utils as utils
from typing import List
import chromadb
from PIL import Image
from collections import deque


class ExploreNode:
    def __init__(self, page_context: PageContext):
        self.id = utils.generate_short_id()
        self.page_context = page_context
        self.links: List[ExploreLink] = []
        self.is_current = False
        self.is_root = False
        page_context.update_node_id(self.id)

    def __repr__(self):
        cur_indicator = "* " if self.is_current else ""
        node_type = "Root" if self.is_root else "Node"
        return f'{cur_indicator}{node_type}({self.page_context.scenario_category}/{self.id} {self.page_context.top_activity}, {len(self.links)} links)'

    def __eq__(self, other):
        return self.id == other.id

    def __hash__(self):
        return hash(self.id)

    def print(self, level, visited_ids, logger=None):
        if self.id in visited_ids:
            return
        visited_ids.add(self.id)
        indent = "\t" * level
        if self.is_root or self.is_current:
            if logger:
                logger.info(f"{indent}{self}")
            else:
                print(f"{indent}{self}")
        for link in self.links:
            if logger:
                logger.info(f"{indent}  └─ {link}")
            else:
                print(f"{indent}  └─ {link}")
            link.target.print(level + 1, visited_ids, logger)

    def traverse(self, level, visited_ids, comparator):
        if self.id in visited_ids:
            return None
        visited_ids.add(self.id)
        if comparator(self):
            return self
        for link in self.links:
            result = link.target.traverse(level + 1, visited_ids, comparator)
            if result:
                return result


class ExploreLink:
    def __init__(self, action_plan: ActionPlan = None, target: ExploreNode = None):
        self.id = utils.generate_short_id()
        self.action_plan = action_plan
        self.target = target

    def __repr__(self):
        return f'Link({self.action_plan} -> Target: {self.target})'


class ExplorePathTree:
    def __init__(self, root: ExploreNode = None):
        self.root = root
        root.is_root = True
        self.current = root
        self.node_map = {}
        self.set_current(root)
        self.logger = utils.build_file_logger("tree_memory")

    def print_tree(self):
        visited_ids = set()
        self.root.print(0, visited_ids, self.logger)
        path = self.find_shortest_path()
        self.logger.info(f"Path: {len(path)}")
        for node in path:
            self.logger.info(f"\tNode: {node}")

    def get_node_count(self):
        return len(self.node_map)

    def set_current(self, current):
        if current:
            if self.current:
                self.current.is_current = False
            self.current = current
            current.is_current = True

    def traverse_nodes(self, comparator) -> ExploreNode:
        visited_ids = set()
        return self.root.traverse(0, visited_ids, comparator)

    def find_shortest_path(self):
        if self.root is None:
            return []

        # BFS setup
        queue = deque([(self.root, [self.root])])
        visited = set()
        visited.add(self.root)

        while queue:
            node, path = queue.popleft()

            # Check if it's the current node
            if node == self.current:
                return path

            # Explore each connected node that hasn't been visited
            for link in node.links:
                target = link.target
                if target not in visited:
                    visited.add(target)
                    queue.append((target, path + [target]))

        return []  # If no path found


class MemoryUpdateStatus:
    def __init__(self):
        self.new_link_added = False
        self.new_node_added = False


class ExploreMemory:
    def __init__(self, tag):
        self.tag = tag
        self.chroma_client = chromadb.Client()
        # if we are going to use the persistent client, we need also save the node graph
        # self.chroma_client = chromadb.PersistentClient(path="./chromac")
        self.page_collection = self.chroma_client.create_collection(name=f"page_memory_{tag}")
        self.element_collection = self.chroma_client.create_collection(name=f"element_memory_{tag}")
        self.explore_tree = None
        self.node_map = {}
        self.page_doc_distance_threshold = 0.03
        self.logger = utils.build_file_logger("explore_memory")

    def fetch_effective_move(self, current_page_context: PageContext,
                             explore_instructions: ExploreInstructions) -> ActionPlan:
        return None

    def add(self, previous_page_context: PageContext, action_plan: ActionPlan,
            current_page_context: PageContext) -> MemoryUpdateStatus:
        update_status = MemoryUpdateStatus()
        if not self.explore_tree:
            root = self.get_node_by_page_and_save_not_exist(previous_page_context)[0]
            self.explore_tree = ExplorePathTree(root)
        if not self.explore_tree.current or not self.explore_tree.current.page_context.node_id == previous_page_context.node_id:
            self.explore_tree.current, previous_already_exist = self.get_node_by_page_and_save_not_exist(
                previous_page_context)
        # and then find the node that is mapped to current_page_context
        current_page_node, current_already_exist = self.get_node_by_page_and_save_not_exist(current_page_context)
        update_status.new_node_added = not current_already_exist

        should_create_link = True
        if current_already_exist and len(self.explore_tree.current.links) > 0:
            for link in self.explore_tree.current.links:
                if link.target.id == current_page_node.id:
                    if link.action_plan.compare_action_content(action_plan):
                        self.logger.info(f"link already exist, skip")
                        should_create_link = False
                    # else:
                    #     self.logger.info(f"link already exist, but with different action plan, adding")
                    #     self.logger.info(f"\t Link action: {link.action_plan}")
                    #     self.logger.info(f"\t New action: {action_plan}")

        if should_create_link:
            update_status.new_link_added = True
            self.logger.info(f"link not exist, adding")
            self.explore_tree.current.links.append(ExploreLink(action_plan, current_page_node))

        self.explore_tree.set_current(current_page_node)

        self.explore_tree.print_tree()
        return update_status

    def query_page(self, query_texts, n_results=1):
        return self.page_collection.query(query_texts=query_texts, n_results=n_results)

    def query_element(self, query_texts, n_results=1):
        return self.element_collection.query(query_texts=query_texts, n_results=n_results)

    def locate_page_by_descriptor(self, page_context):
        query_result = self.query_page(query_texts=page_context.get_page_descriptor(), n_results=1)
        if len(query_result['documents'][0]) == 1:
            distance = query_result['distances'][0][0]
            find_page_id = query_result['ids'][0][0]
            if distance < self.page_doc_distance_threshold:
                # find_page_doc = query_result['documents'][0][0]
                return find_page_id
            else:
                self.logger.info(f"Deny page {find_page_id} with distance {distance}")
        return None

    def get_node_by_page_and_save_not_exist(self, page_context: PageContext):
        find_page_id = self.locate_page_by_descriptor(page_context)
        if find_page_id:
            self.logger.info(f"find page {find_page_id} for page: {page_context.get_page_descriptor()}")
            return self.node_map[find_page_id], True
        new_node = ExploreNode(page_context)
        self.node_map[new_node.id] = new_node
        self.save_page_context_vector(page_context)
        return new_node, False

    def save_page_context_vector(self, page_context: PageContext):
        self.page_collection.add(documents=[page_context.get_page_descriptor()],
                                 metadatas=[{"key_elements_count": len(page_context.key_elements)}],
                                 ids=[page_context.node_id])

    def report_explore_note(self):
        return ""

    def locate_page_by_screenshot(self, screenshot_file, threshold=0.01, width=360) -> PageContext:
        if self.explore_tree is None:
            return None
        compare_image = None
        with Image.open(screenshot_file) as screenshot:
            compare_image = utils.resize_image_by_width(screenshot, width)

        def callback(node: ExploreNode) -> bool:
            with node.page_context.load_screenshot_image() as node_screenshot:
                with utils.resize_image_by_width(node_screenshot, width) as resized_image:
                    diff_ratio = utils.get_image_difference_ratio(resized_image, compare_image)
            if diff_ratio < threshold:
                self.logger.info(f"Found match node: {node.id}")
                return True
            else:
                return False

        found = self.explore_tree.traverse_nodes(callback)
        if found:
            return found.page_context
        return None
