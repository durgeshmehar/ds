
**Core Concepts Involved:**

1.  **RMI (Remote Method Invocation):** This is the fundamental Java technology used. It allows an object running in one Java Virtual Machine (JVM) – the client – to call methods on an object running in a different JVM – the server. This communication can happen between processes on the same machine or across different machines on a network. It makes distributed programming feel similar to local programming.
2.  **Remote Interface (`AddServerIntf.java`):**
    *   Defines the *contract* between the client and the server. It lists the methods that the client can call remotely.
    *   Must extend the `java.rmi.Remote` interface (a marker interface).
    *   All methods declared in the remote interface must declare `throws java.rmi.RemoteException`. This exception is used to signal potential network or RMI-related errors during the remote call.
    *   In your case, it defines the `double add(double d1, double d2)` method.
3.  **Remote Object Implementation (`AddServerImpl.java`):**
    *   This class provides the *actual code* that runs on the server to execute the methods defined in the remote interface.
    *   It implements the remote interface (`AddServerIntf`).
    *   It typically extends `java.rmi.server.UnicastRemoteObject`. Extending this class handles the underlying RMI communication details like listening for incoming client requests, marshalling (packaging) arguments to send over the network, unmarshalling (unpacking) results, etc. The constructor must also declare `throws RemoteException`.
    *   Your `AddServerImpl` implements the `add` method to simply return the sum of the two input doubles.
4.  **RMI Registry:**
    *   A simple naming service provided by RMI. Think of it like a phonebook for remote objects.
    *   The server uses the registry to *register* (or *bind*) its remote object under a specific, publicly known name (e.g., "AddServer").
    *   Clients *look up* this name in the registry to get a reference (a stub) to the remote object.
    *   It usually runs as a separate process, started with the `rmiregistry` command.
5.  **Server Application (`AddServer.java`):**
    *   The main program that runs on the server machine.
    *   Its primary jobs are:
        *   Create an instance of the remote object implementation (`AddServerImpl`).
        *   Register this instance with the RMI registry using `java.rmi.Naming.rebind("AddServer", addServerImpl)`. `rebind` will overwrite any existing binding with that name, while `bind` would throw an exception if the name is already taken.
    *   Once bound, the server implicitly waits for clients to connect and invoke methods.
6.  **Client Application (`AddClient.java`):**
    *   The main program that runs on the client machine (or another process on the same machine).
    *   Its primary jobs are:
        *   Construct the URL for the remote object using the server's address and the registered name (e.g., `"rmi://127.0.0.1/AddServer"`).
        *   Use `java.rmi.Naming.lookup(addServerURL)` to contact the RMI registry at the specified address and request the object named "AddServer".
        *   The `lookup` method returns a *stub* object. This stub is a client-side proxy that implements the same remote interface (`AddServerIntf`) as the actual server object.
        *   The client calls methods (like `add(d1, d2)`) on this stub object *as if it were the real object*.
        *   The stub handles the network communication: marshalling the arguments, sending the request to the server, receiving the result, unmarshalling it, and returning it to the client code.
7.  **Stub/Skeleton (Behind the Scenes):** RMI uses proxy objects. The client gets a *stub*, and the server uses a *skeleton* (though skeletons are less explicit in modern Java). The stub packages the method call and sends it over the network. The skeleton on the server receives the request, unpacks it, calls the actual method on the `AddServerImpl` object, packages the result, and sends it back. The `rmic` command was traditionally used to generate these.

**Explanation of Commands (Windows):**

1.  `javac *.java`
    *   `javac`: The command to invoke the Java compiler.
    *   `*.java`: A wildcard pattern telling the compiler to process all files in the current directory ([`Assign1`](Assign1 )) that end with the `.java` extension.
    *   **Action:** Compiles your source code files (`AddClient.java`, `AddServer.java`, `AddServerImpl.java`, `AddServerIntf.java`) into Java bytecode files (`.class` files), which the Java Virtual Machine (JVM) can execute. You'll see files like `AddClient.class`, `AddServer.class`, etc., appear.

2.  `rmic AddServerImpl`
    *   `rmic`: The command to invoke the RMI stub compiler.
    *   `AddServerImpl`: The name of the *compiled* remote object implementation class (without the `.class` extension).
    *   **Action:** Reads the `AddServerImpl.class` file. Traditionally, this command generated the client-side stub (`AddServerImpl_Stub.class`) and potentially a server-side skeleton class. The stub is the proxy object the client interacts with. *Note: Since Java 1.5, explicit stub generation with `rmic` is often unnecessary, as RMI can generate stubs dynamically. However, running it ensures compatibility or fulfills specific requirements.* You can see it generated `AddServerImpl_Stub.class` in your folder.

3.  `rmiregistry`
    *   `rmiregistry`: The command to start the RMI registry service.
    *   **Action:** Launches a separate process that listens for RMI naming operations (binding and lookup), typically on TCP port 1099 by default. The server (`AddServer`) needs this registry running *before* it can bind its remote object. The client (`AddClient`) contacts this registry to find the server object. You usually run this in its own dedicated command prompt window, from the directory containing the compiled classes.

4.  `java AddServer`
    *   `java`: The command to invoke the Java Virtual Machine (JVM) to run a compiled Java program.
    *   `AddServer`: The name of the class containing the `main` method to execute (your server application's entry point).
    *   **Action:** Starts the server application. The code inside `AddServer.java`'s `main` method executes: it creates an `AddServerImpl` object and registers it with the name "AddServer" in the RMI registry started in the previous step. The server process then stays running, listening for incoming remote method calls from clients.

5.  `java AddClient 127.0.0.1 5 8`
    *   `java`: Invokes the JVM.
    *   `AddClient`: The name of the class containing the client application's `main` method.
    *   `127.0.0.1 5 8`: These are command-line arguments passed to the `main` method's `String args[]` parameter.
        *   `args[0]` becomes `"127.0.0.1"`: The IP address or hostname where the RMI registry and server are running. `127.0.0.1` is the standard loopback address, meaning "this same machine".
        *   `args[1]` becomes `"5"`: The first number to be added.
        *   `args[2]` becomes `"8"`: The second number to be added.
    *   **Action:** Starts the client application. The client code uses `args[0]` ("127.0.0.1") to look up "AddServer" in the RMI registry. It gets the stub, parses `args[1]` ("5") and `args[2]` ("8") into doubles, calls the `add` method on the stub, receives the result (13.0) back from the server via RMI, and prints the output. The client program then typically terminates.