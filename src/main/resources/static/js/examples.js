// Example programs shown in the "示例代码" menu.
window.EXAMPLES = [
    {
        name: "Hello World",
        source: `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, OnlineExecutor!");
    }
}
`,
    },
    {
        name: "读取标准输入",
        stdin: "3\n10 20 12\n",
        source: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += in.nextInt();
        }
        System.out.println("sum = " + sum);
    }
}
`,
    },
    {
        name: "Stream 与 record",
        source: `import java.util.*;
import java.util.stream.*;

public class Main {
    record Score(String name, int value) {}

    public static void main(String[] args) {
        List<Score> scores = List.of(
                new Score("Alice", 92), new Score("Bob", 78), new Score("Carol", 85));

        String ranking = scores.stream()
                .sorted(Comparator.comparingInt(Score::value).reversed())
                .map(s -> s.name() + "=" + s.value())
                .collect(Collectors.joining(", "));
        System.out.println(ranking);

        IntSummaryStatistics stats = scores.stream().mapToInt(Score::value).summaryStatistics();
        System.out.println(stats);
    }
}
`,
    },
    {
        name: "斐波那契",
        source: `public class Main {
    public static void main(String[] args) {
        for (int i = 0; i < 15; i++) {
            System.out.print(fib(i) + " ");
        }
        System.out.println();
    }

    static long fib(int n) {
        return n < 2 ? n : fib(n - 1) + fib(n - 2);
    }
}
`,
    },
    {
        name: "未捕获异常",
        source: `public class Main {
    public static void main(String[] args) {
        int[] values = {1, 2, 3};
        System.out.println("about to fail");
        System.out.println(values[3]);
    }
}
`,
    },
    {
        name: "编译错误",
        source: `public class Main {
    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        System.out.println(names)
    }
}
`,
    },
    {
        name: "超时（死循环）",
        source: `public class Main {
    public static void main(String[] args) {
        long n = 0;
        while (true) {
            n++;
        }
    }
}
`,
    },
];
