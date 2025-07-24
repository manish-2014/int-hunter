How setInt Detection Works - Detailed Explanation
The Bytecode Pattern
When Java code uses PreparedStatement, it follows a predictable pattern in bytecode:

SQL String Loading: The SQL string is loaded onto the stack using LDC (Load Constant)
PrepareStatement Call: The Connection's prepareStatement method is invoked
Store PreparedStatement: The returned PreparedStatement is stored in a local variable
Setter Calls: The PreparedStatement's setter methods (like setInt) are called

Step-by-Step Detection Process

SQL Detection Phase:

We scan for LDC opcodes that load string constants
We check if these strings look like SQL (start with SELECT, INSERT, etc.)
We store the most recent SQL string we've seen


PrepareStatement Detection:

We look for INVOKEINTERFACE or INVOKEVIRTUAL opcodes
We check if the method being called is prepareStatement on a Connection
We note which local variable the PreparedStatement is stored in (via ASTORE)


Setter Method Detection:

We continue scanning forward in the bytecode
We look for method calls on PreparedStatement objects
We check if these are setter methods (setInt, setString, etc.)
When found, we create a Finding to report this usage



Why This Works
The bytecode preserves the sequence of operations, so we can reliably track:

When SQL strings are loaded
When they're used to create PreparedStatements
When setter methods are called on those PreparedStatements

This allows us to identify parameterized queries, which are the recommended way to prevent SQL injection vulnerabilities.Add to Conversation

