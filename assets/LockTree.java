import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LockTree {
    public static final String RMode = "R";
    public static final String WMode = "W";
    public static final String wildcard = "*";
    private int size = 0;
    public LockTree(int size) {
        this.size = size;
    }
    private static class Node {
        String name;
        String mode;
        Node parent;
        boolean mark = false;
        Map<String, Node> children = new HashMap<>();

        public Node(String name, String mode) {
            this.name = name;
            this.mode = mode;
        }

        public void deleteR() {
            List<String> toDelete = new ArrayList<>();
            for(String k : children.keySet()) {
                if(!children.get(k).mark) {
                    toDelete.add(k);
                }else{
                    children.get(k).deleteR();
                }
            }
            for(String k : toDelete) {
                children.remove(k);
            }
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public boolean cover(Node another) {
            if(mode.equals(WMode) && another.mode.equals(RMode)) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer(mode + name);
            return sb.toString();
        }
    }

    private Node root = new Node("ROOT: ", RMode);

    //merge the given branch into the tree
    public void apply(List<Node> branch) {
        if(branch.size() != this.size) {
            throw new IllegalArgumentException("size not right");
        }
        Node parent = root;
        for(int i = 0; i < branch.size(); i++) {
            Node node = branch.get(i);
            Map<String, Node> current = parent.children;
            boolean add = false;
            //main logic
            if(!node.name.equals(wildcard) && !current.containsKey(wildcard)) {
                if(!current.containsKey(node.name)) {
                    add = true;
                } else if(current.containsKey(node.name) && node.cover(current.get(node.name))) {
                    current.get(node.name).mode = node.mode;
                }
            } else if(!node.name.equals(wildcard) && current.containsKey(wildcard)) {
                if(current.get(wildcard).mark || !node.mark) {
                    return;
                }
                if(!current.containsKey(node.name)) {
                    add = true;
                }
            } else if(node.name.equals(wildcard) && !current.containsKey(wildcard)) {
                if(node.mark) {
                    current.clear();
                }else{
                    parent.deleteR();
                }
                add = true;
            } else if(node.name.equals(wildcard) && current.containsKey(wildcard)) {
                if(node.mark && !current.get(wildcard).mark) {
                    current.clear();
                    add = true;
                }else{
                    return;
                }
            }

            if(add) {
                node.setParent(parent);
                current.put(node.name, node);
            }

            //mark the node no matter we add new node or not
            if(node.mark) {
                current.get(node.name).mark = true;
            }

            //step further
            parent = current.get(node.name);
        }
    }

    public void traverse(Node node, String path) {
        if(node.children.size()>0) {
            for(String k : node.children.keySet()) {
                traverse(node.children.get(k), path + node.name);
            }
        }else{
            path += node.name + "[" + node.mode + "]";
            System.out.println(path);
        }
    }

    public static List<Node> parseUnixPath(String unixPath, String mode) {
        List<Node> nodes = new ArrayList<>();
        if(unixPath.startsWith("/")) {
            unixPath = unixPath.substring(1);
        }
        if(unixPath.endsWith("/")) {
            unixPath = unixPath.substring(0, unixPath.length() - 1);
        }

        for(String p : unixPath.split("/")) {
            Node newNode = new Node(p, RMode);
            if(mode.equals(WMode)) {
                newNode.mark = true;
            }
            nodes.add(newNode);
        }
        if(nodes.size() > 0) {
            nodes.get(nodes.size()-1).mode = mode;
        }
        return nodes;
    }

    public static void printBranch(List<Node> branch) {
        StringBuilder sb = new StringBuilder("/");
        for(int i = 0; i < branch.size(); i++) {
            sb.append(branch.get(i).name + "/");
        }
        System.out.println(branch.get(branch.size()-1).mode + sb.substring(0,sb.length()-1));
    }

    public static void main(String[] args) {
        //R/a/b/c; R/a/b/d; R/a/e/f; W/a/b/c; R/a/b/e; R/a/b/*; R/a/*/*; W/a/*/*
        List<Node> branch1 = parseUnixPath("/a/b/c", RMode);
        List<Node> branch2 = parseUnixPath("/a/b/d", RMode);
        List<Node> branch3 = parseUnixPath("/a/e/f", RMode);
        List<Node> branch4 = parseUnixPath("/a/b/c", WMode);
        List<Node> branch5 = parseUnixPath("/a/b/e", RMode);
        List<Node> branch6 = parseUnixPath("/a/b/*", RMode);
        List<Node> branch7 = parseUnixPath("/a/*/*", RMode);
        List<Node> branch8 = parseUnixPath("/a/*/*", WMode);
        LockTree tree = new LockTree(3);

        printBranch(branch1);
        tree.apply(branch1);
        tree.traverse(tree.root, "");
        printBranch(branch2);
        tree.apply(branch2);
        tree.traverse(tree.root, "");
        printBranch(branch3);
        tree.apply(branch3);
        tree.traverse(tree.root, "");
        printBranch(branch4);
        tree.apply(branch4);
        tree.traverse(tree.root, "");
        printBranch(branch5);
        tree.apply(branch5);
        tree.traverse(tree.root, "");
        printBranch(branch6);
        tree.apply(branch6);
        tree.traverse(tree.root, "");
        printBranch(branch7);
        tree.apply(branch7);
        tree.traverse(tree.root, "");
        printBranch(branch8);
        tree.apply(branch8);
        tree.traverse(tree.root, "");
    }

}
