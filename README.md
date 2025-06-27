Javadocs link: {TBA}<br/>
Only a few examples are introduced below, it would be better to check the javadocs and/or the code 

Lightweight (~40kb), linear-time java parser that supports custom operators, functions, replaceable variables, etc

```java
Parser parser = new Parser();
Formula formula = parser.parse("17.43+((3.5+2.1*(7-4%3))^2.3)/((6+4.2+(8%5)))");
System.out.println(formula.inOperationOrderResult());
```

Parser object holds configuration and caching state.</br>
There exist the 'ParserStatic' class that wraps default methods around a global parser for convenience.
```java
Formula formula = StaticParser.parse("17.43+((3.5+2.1*(7-4%3))^2.3)/((6+4.2+(8%5)))");
System.out.println(formula.inOperationOrderResult());
```

## Installation
### Maven
```xml
<dependency>
    <groupId>io.github.gochi9</groupId>
    <artifactId>JLSP</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle
```groovy
implementation("io.github.gochi9:JLSP:1.0")
```


<br/>

## Default operators / functions / commas / delimiters
<details>
<summary>Show</summary>

```java
addOperator('^', (a, b, extra) -> {
double base = Math.abs(a);
int sign = 1;

if (extra[0] && extra[1]) {
    if (b != Math.floor(b))
        throw new ArithmeticException("cannot raise negative number to fractional power");
    if ((long) b % 2 != 0)
        sign = -sign;
}
else if (!extra[0] && a < 0)
    sign = -sign;

if (extra[0] && extra[2])     
    sign = -sign;
return sign * Math.pow(base, b);}, 10);

addOperator('*', (a, b) -> a * b, 5);
addOperator('/', (a, b) -> a / b, 5);
addOperator('%', (a, b) -> a % b, 5);

addOperator('+', (a, b) -> a + b, 0);
addOperator('-', (a, b) -> a - b, 0);

addComma('.');

addDelimiter(',');
```

```java
addFunction("pi", (caller, inOperationOrder, a) -> Math.PI);
addFunction("e", (caller, inOperationOrder, a) -> Math.E);
addFunction("phi", (caller, inOperationOrder, a) -> (1 + Math.sqrt(5)) / 2);
addFunction("tau", (caller, inOperationOrder, a) -> 2 * Math.PI);
addFunction("sqrt2", (caller, inOperationOrder, a) -> Math.sqrt(2));
addFunction("sqrt3", (caller, inOperationOrder, a) -> Math.sqrt(3));
addFunction("ln2", (caller, inOperationOrder, a) -> Math.log(2));
addFunction("ln10", (caller, inOperationOrder, a) -> Math.log(10));
addFunction("log2e", (caller, inOperationOrder, a) -> 1 / Math.log(2));
addFunction("log10e", (caller, inOperationOrder, a) -> 1 / Math.log(10));
addFunction("inf", (caller, inOperationOrder, a) -> Double.POSITIVE_INFINITY);
addFunction("nan", (caller, inOperationOrder, a) -> Double.NaN);
```
'B-b-but PI isn't a function'<br/>
It is defined like an 0-arg function here for clarity. In theory, with this setup, you could use 1+pi+pi() where simple pi would be translated 1 + p*i + pi() where p and i are two individual separate entities that can be replaced.<br/>
'B-b-but isn't that confusing?'<br/>
Keen observation. Too bad though. Just use π instead

<br/><br/>

```java
        addFunction("abs", (caller, inOperationOrder, a) -> Math.abs(processEntity(caller, a[0], inOperationOrder)));
        addFunction("round", (caller, inOperationOrder, a) -> (double) Math.round(processEntity(caller, a[0], inOperationOrder)));
        addFunction("floor", (caller, inOperationOrder, a) -> Math.floor(processEntity(caller, a[0], inOperationOrder)));
        addFunction("ceil", (caller, inOperationOrder, a) -> Math.ceil(processEntity(caller, a[0], inOperationOrder)));
        addFunction("mod", (caller, inOperationOrder, a) -> processEntity(caller, a[0], inOperationOrder) % processEntity(caller, a[1], inOperationOrder));
        addFunction("sqrt", (caller, inOperationOrder, a) -> Math.sqrt(processEntity(caller, a[0], inOperationOrder)));
        addFunction("pow", (caller, inOperationOrder, a) -> Math.pow(processEntity(caller, a[0], inOperationOrder), processEntity(caller, a[1], inOperationOrder)));
        addFunction("exp", (caller, inOperationOrder, a) -> Math.exp(processEntity(caller, a[0], inOperationOrder)));
        addFunction("log", (caller, inOperationOrder, a) -> Math.log(processEntity(caller, a[0], inOperationOrder)));
        addFunction("log10", (caller, inOperationOrder, a) -> Math.log10(processEntity(caller, a[0], inOperationOrder)));
        addFunction("log2", (caller, inOperationOrder, a) -> Math.log(processEntity(caller, a[0], inOperationOrder)) / Math.log(2));
        addFunction("sin", (caller, inOperationOrder, a) -> Math.sin(processEntity(caller, a[0], inOperationOrder)));
        addFunction("cos", (caller, inOperationOrder, a) -> Math.cos(processEntity(caller, a[0], inOperationOrder)));
        addFunction("tan", (caller, inOperationOrder, a) -> Math.tan(processEntity(caller, a[0], inOperationOrder)));
        addFunction("asin", (caller, inOperationOrder, a) -> Math.asin(processEntity(caller, a[0], inOperationOrder)));
        addFunction("acos", (caller, inOperationOrder, a) -> Math.acos(processEntity(caller, a[0], inOperationOrder)));
        addFunction("atan", (caller, inOperationOrder, a) -> Math.atan(processEntity(caller, a[0], inOperationOrder)));
        addFunction("atan2", (caller, inOperationOrder, a) -> Math.atan2(processEntity(caller, a[0], inOperationOrder), processEntity(caller, a[1], inOperationOrder)));
        addFunction("sinh", (caller, inOperationOrder, a) -> Math.sinh(processEntity(caller, a[0], inOperationOrder)));
        addFunction("cosh", (caller, inOperationOrder, a) -> Math.cosh(processEntity(caller, a[0], inOperationOrder)));
        addFunction("tanh", (caller, inOperationOrder, a) -> Math.tanh(processEntity(caller, a[0], inOperationOrder)));
        addFunction("asinh", (caller, inOperationOrder, a) -> Math.log(processEntity(caller, a[0], inOperationOrder) + Math.sqrt(Math.pow(processEntity(caller, a[0], inOperationOrder), 2) + 1)));
        addFunction("acosh", (caller, inOperationOrder, a) -> Math.log(processEntity(caller, a[0], inOperationOrder) + Math.sqrt(Math.pow(processEntity(caller, a[0], inOperationOrder), 2) - 1)));
        addFunction("atanh", (caller, inOperationOrder, a) -> 0.5 * Math.log((1 + processEntity(caller, a[0], inOperationOrder)) / (1 - processEntity(caller, a[0], inOperationOrder))));

        addFunction("fact", (caller, inOperationOrder, a) -> {
            int val = (int) processEntity(caller, a[0], inOperationOrder);
            
            if (val < 0) 
                throw new IllegalArgumentException("factorial undefined for negative");
            
            long result = 1L;
            for (int i = 1; i <= val; i++) 
                result *= i;
            
            return (double) result;
        });
        addFunction("min", (caller, inOperationOrder, a) -> {
            double result = Double.POSITIVE_INFINITY;
            for (int i = 0; i < a.length; i++) {
                double val = processEntity(caller, a[i], inOperationOrder);
                if (val < result) result = val;
            }
            return result;
        });
        addFunction("max", (caller, inOperationOrder, a) -> {
            double result = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < a.length; i++) {
                double val = processEntity(caller, a[i], inOperationOrder);
                if (val > result) result = val;
            }
            return result;
        });
        addFunction("avg", (caller, inOperationOrder, a) -> {
            double sum = 0;
            for (int i = 0; i < a.length; i++)
                sum += processEntity(caller, a[i], inOperationOrder);
            return sum / a.length;
        });
        addFunction("sum", (caller, inOperationOrder, a) -> {
            double total = 0;
            for (int i = 0; i < a.length; i++)
                total += processEntity(caller, a[i], inOperationOrder);
            return total;

        });
        addFunction("median", (caller, inOperationOrder, a) -> {
            double[] vals = new double[a.length];
            for (int i = 0; i < a.length; i++) vals[i] = processEntity(caller, a[i], inOperationOrder);
            Arrays.sort(vals);
            int mid = a.length / 2;
            return a.length % 2 == 1 ? vals[mid] : (vals[mid - 1] + vals[mid]) / 2.0;
        });
        addFunction("sign", (caller, inOperationOrder, a) -> Math.signum(processEntity(caller, a[0], inOperationOrder)));
        addFunction("deg", (caller, inOperationOrder, a) -> Math.toDegrees(processEntity(caller, a[0], inOperationOrder)));
        addFunction("rad", (caller, inOperationOrder, a) -> Math.toRadians(processEntity(caller, a[0], inOperationOrder)));
        addFunction("clamp", (caller, inOperationOrder, a) -> {
            double val = processEntity(caller, a[0], inOperationOrder);
            double min = processEntity(caller, a[1], inOperationOrder);
            double max = processEntity(caller, a[2], inOperationOrder);
            return Math.max(min, Math.min(max, val));
        });
        addFunction("brt", (caller, inOperationOrder, a) -> Math.cbrt(processEntity(caller, a[0], inOperationOrder)));
        addFunction("nthroot", (caller, inOperationOrder, a) -> Math.pow(processEntity(caller, a[0], inOperationOrder), 1.0 / processEntity(caller, a[1], inOperationOrder)));
        addFunction("hypot", (caller, inOperationOrder, a) -> Math.hypot(processEntity(caller, a[0], inOperationOrder), processEntity(caller, a[1], inOperationOrder)));
        addFunction("root", (caller, inOperationOrder, a) -> Math.pow(processEntity(caller, a[0], inOperationOrder), 1.0 / processEntity(caller, a[1], inOperationOrder)));
        addFunction("bitand", (caller, inOperationOrder, a) -> (double)((long)processEntity(caller, a[0], inOperationOrder) & (long)processEntity(caller, a[1], inOperationOrder)));
        addFunction("bitor", (caller, inOperationOrder, a) -> (double)((long)processEntity(caller, a[0], inOperationOrder) | (long)processEntity(caller, a[1], inOperationOrder)));
        addFunction("bitxor", (caller, inOperationOrder, a) -> (double)((long)processEntity(caller, a[0], inOperationOrder) ^ (long)processEntity(caller, a[1], inOperationOrder)));
        addFunction("bitnot", (caller, inOperationOrder, a) -> (double)(~(long)processEntity(caller, a[0], inOperationOrder)));
        addFunction("shl", (caller, inOperationOrder, a) -> (double) ((long) processEntity(caller, a[0], inOperationOrder) << (int) (long) processEntity(caller, a[1], inOperationOrder)));
        addFunction("shr", (caller, inOperationOrder, a) -> (double) ((long) processEntity(caller, a[0], inOperationOrder) >> (int) (long) processEntity(caller, a[1], inOperationOrder)));
        addFunction("sec", (caller, inOperationOrder, a) -> 1.0 / Math.cos(processEntity(caller, a[0], inOperationOrder)));
        addFunction("csc", (caller, inOperationOrder, a) -> 1.0 / Math.sin(processEntity(caller, a[0], inOperationOrder)));
        addFunction("cot", (caller, inOperationOrder, a) -> 1.0 / Math.tan(processEntity(caller, a[0], inOperationOrder)));
        addFunction("normalize_angle", (caller, inOperationOrder, a) -> {
            double angle = processEntity(caller, a[0], inOperationOrder);
            double twoPi = 2 * Math.PI;
            return ((angle % twoPi) + twoPi) % twoPi;
        });
        addFunction("wrap", (caller, inOperationOrder, a) -> {
            double val = processEntity(caller, a[0], inOperationOrder);
            double min = processEntity(caller, a[1], inOperationOrder);
            double max = processEntity(caller, a[2], inOperationOrder);
            double range = max - min;
            return ((val - min) % range + range) % range + min;
        });
        Random randInstance = new Random();
        addFunction("rand", (caller, inOperationOrder, a) -> randInstance.nextDouble());
        addFunction("randint", (caller, inOperationOrder, a) -> {
            int min = (int)processEntity(caller, a[0], inOperationOrder);
            int max = (int)processEntity(caller, a[1], inOperationOrder);
            return min + randInstance.nextInt(max - min + 1);
        });
        addFunction("randrange", (caller, inOperationOrder, a) -> {
            double min = processEntity(caller, a[0], inOperationOrder);
            double max = processEntity(caller, a[1], inOperationOrder);
            return min + (max - min) * randInstance.nextDouble();
        });
        addFunction("seed", (caller, inOperationOrder, a) -> {
            randInstance.setSeed((long)processEntity(caller, a[0], inOperationOrder));
            return 0.0;
        });
        addFunction("noise", (caller, inOperationOrder, a) -> {
            double x = processEntity(caller, a[0], inOperationOrder);
            return (Math.sin(x * 12.9898 + 78.233) + 1.0) * 0.5;
        });
        addFunction("dot", (caller, inOperationOrder, a) -> processEntity(caller, a[0], inOperationOrder) * processEntity(caller, a[1], inOperationOrder));
        addFunction("cross", (caller, inOperationOrder, a) -> processEntity(caller, a[0], inOperationOrder) * processEntity(caller, a[1], inOperationOrder));
        addFunction("length", (caller, inOperationOrder, a) -> Math.abs(processEntity(caller, a[0], inOperationOrder)));
        addFunction("normalize", (caller, inOperationOrder, a) -> {
            double val = processEntity(caller, a[0], inOperationOrder);
            return val == 0 ? 0 : val / Math.abs(val);
        });
        addFunction("distance", (caller, inOperationOrder, a) -> {
            double dx = processEntity(caller, a[0], inOperationOrder) - processEntity(caller, a[1], inOperationOrder);
            return Math.abs(dx);
        });
        addFunction("angle", (caller, inOperationOrder, a) -> {
            double x = processEntity(caller, a[0], inOperationOrder);
            double y = processEntity(caller, a[1], inOperationOrder);
            return Math.atan2(y, x);
        });
        addFunction("lerp", (caller, inOperationOrder, a) -> {
            double a0 = processEntity(caller, a[0], inOperationOrder);
            double a1 = processEntity(caller, a[1], inOperationOrder);
            double t = processEntity(caller, a[2], inOperationOrder);
            return a0 + (a1 - a0) * t;
        });
        addFunction("mix", (caller, inOperationOrder, a) -> {
            double a0 = processEntity(caller, a[0], inOperationOrder);
            double a1 = processEntity(caller, a[1], inOperationOrder);
            double t = processEntity(caller, a[2], inOperationOrder);
            return a0 + (a1 - a0) * t;
        });
```

</details>

## Adding custom operators

Use:

```java
addOperator(char symbol, DoubleBinaryOperator compute, int priority);
```
Example:
```java
addOperator('*', (left, right, extra) -> a * b, 5);
addOperator('+', (left, right, extra) -> a + b, 0);
```
priority controls evaluation order. a higher value is evaluated first, so ^ comes before * and /, which come before + and -

To modify or remove an operator:

```java
changeOperatorPriority(char symbol, int priority);
removeOperator(char symbol);
// more in the Javadocs
```

You can raise the priority of + above ^ or remove * entirely.

## Custom functions

FunctionCompute addFunction(String id, FunctionCompute compute);
// more in the Javadocs

FunctionCompute.compute receives

1. the calling Formula
2. a flag that tells you whether evaluation runs in operation order or flat
3. the array of FormulaEntity objects passed to the function

```java
public interface FunctionCompute {
    double compute(Formula caller, boolean inOperationOrder, FormulaEntity<?>... entities);
}
```


### Simple function

```java
addFunction("abs", (caller, inOrder, e) -> Math.abs(processEntity(caller, e[0], inOrder)));
```
### Function with validation

```java
addFunction("abs", (caller, inOrder, e) -> {
    if (e.length == 0)
        throw new UnsupportedOperationException("Invalid number of parameters");
    return Math.abs(processEntity(caller, e[0], inOrder));
});
```

### Multiple parameters

```java
addFunction("nthroot", (caller, inOrder, e) -> Math.pow(processEntity(caller, e[0], inOrder),1.0 / processEntity(caller, e[1], inOrder)));
```
### Loop

```java
addFunction("avg", (caller, inOrder, e) -> {
    double sum = 0;
    for (int i = 0; i < e.length; i++)
        sum += processEntity(caller, e[i], inOrder);
    return sum / e.length;
});
```

### No argument function

```java
addFunction("rand", (caller, inOrder, e) -> randInstance.nextDouble());
```

## Delimiters and commas

Define custom comma characters and delimiters:

addComma(char c);
addDelimiter(char c);
// more in the Javadocs

Commas

* commas = {'.'}  
  2.5

* commas = {'.', '_'}  
  2.5 + 4_8

Delimiters

* delimiters = {','}  
  f(x, y)

* delimiters = {',', '/'}  
  f(x, y/ z)

## Configurable flags

```java
private boolean skipEmptySpace = true;
private char defaultOperator = '+';
private char betweenVariables = '*';
private double defaultValueBetweenEmptyOperators = 0D;
```

skipEmptySpace, when false the parser can treat whitespace as an operator/comma/delimiter/variable placeholder.

defaultOperator, inserted at the start of a formula when no operator is present. for example 2+2 becomes +2+2. if you set it to '-', the input becomes -2+2. the char must be a valid operator.

betweenVariables refers to the default operator that appears between variables. so if you had ab(11+3), then it'd get interpreted as a * b * (11+3). or if betweenVariables = '+', it would become a + b + (11+3)

defaultValueBetweenEmptyOperators, fills gaps between stacked operators. with a default of 0

...More available, check the jadvadocs

```java
-*+--4
```

is read as

```java
-0*0+0-(-4)
```

## Changing the parsing logic

Apart from being able to extend and override most methods in the Parser class, you can also replace the entire parsing logic like this:

```java
parser.setParseLogic(ParseCompute parseLogic);
```

Your custom logic class must implement this:

```java
public interface ParseCompute {
    Formula parse(String source);
}
```
