# Contributing

Thank you for your interest in contributing to LibreLauncherLib! We welcome contributions from developers of all skill levels. Please review these guidelines before submitting code or opening issues.

### Code Style
Use Allman indentation style and camelCase:
```java
public class ExampleClass
{
    private Foo process(String inputParameter)
    {
        if (inputParameter == null)
        {
            throw new IllegalArgumentException("Input parameter cannot be null");
        }

        return new Foo(inputParameter);
    }
}
```
### Documentation

When adding a feature make sure to provide a description for the Javadoc.

```java
/**
 * Executes initialization for Example
 *
 * @param profile The profile options for initialization.
 * @return something that's Example.
 * @throws ExampleException If initialization fails or profile is invalid.
 */
public Example initialize(ExampleProfile profile) throws ExampleException
{
    if (profile == null)
    {
        throw new ExampleException("Profile cannot be null");
    }
    return new Example(profile);
}
```

## License

By contributing to this project, you agree that your contributions will be licensed under the MIT License.