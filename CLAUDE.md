# Fleet Control

FleetControl es un backend de gestión de flotas que se construye en un sprint de 2 semanas y no depende de ninguna API externa: los datos los generan y almacenan los propios servicios.

## Reglas de codificacion

- **Nunca** usar excepciones genericas como `RuntimeException` o `Exception`. Siempre se debe usar una excepción específica que describa el caso de error especifico.
- **Jamas** hacer que las excepciones manejen datos de negocio, codigos de respuesta HTTP, titulo de error. Las excepciones solo deben representar un caso de error especifico (VehicleNotFoundException, EmailAlreadyInUseException, etc) y un mensaje de error que describa el caso de error. Los datos de negocio, codigos de respuesta HTTP, titulo de error deben ser manejados en un nivel superior mediante un ExceptionHandler (Rest Controller Advice).
- **Siempre** usar controller advice para manejar las excepciones y mapearlas a respuestas HTTP adecuadas.
- **Siempre** usar DTOs para las request y response de los endpoints. Nunca exponer entidades de negocio directamente en los endpoints.
- **Siempre** agregar validaciones a los DTOs de request usando anotaciones de validaciones de spring validation (jakarta).
- **Siempre** usar interfaces para la inyeccion de dependencias y para definir los contratos de los servicios. Nunca exponer implementaciones concretas en los controladores.
- **Siempre** usar inyeccion de dependencias por constructor y nunca por campo.
- **Siempre** usar nombres de variables y metodos descriptivos y en ingles. Nunca usar abreviaciones o nombres ambiguos.
- **Siempre** usar MapStruct para mapear entre entidades y DTOs. Nunca hacer el mapeo manualmente.
- **Siempre** usar Lombok para generar getters, setters, constructores y builders. Nunca escribirlos manualmente.
- **Nunca** usar `@Query` para hacer consultar personalizadas para consultas de datos con varios parametros. **Siempre** usar `JpaSpecificationExecutor` para consultas complejas y parametrizadas.
- **Siempre** documentar los endpoints con OpenAPI y Swagger. Nunca dejar endpoints sin documentar.
- **Nunca** usar comentarios en el codigo para explicar su funcionamiento, El codigo debe ser autoexplicativo y legible.
- **Siempre** abstraer la logica de validaciones u operaciones complejas en clases helpers para disminuir la complejidad de los servicios y controladores. Nunca dejar logica compleja en los servicios o controladores.

## Reglas de Git

- **Siempre** usar conventional commits para los mensajes de commit. Nunca usar mensajes de commit ambiguos o poco descriptivos.
- **Siempre** redactar los commits en español. Nunca usar ingles para los mensajes de commit.
- **Siempre** agregar description y body a los commits para explicar el cambio realizado. Nunca dejar commits sin descripcion o body.
- **Nunca** hacer commits que contengan cambios de multiples funcionalidades o features. Siempre hacer commits atomicamente por funcionalidad o feature. Nunca mezclar cambios de multiples funcionalidades en un solo commit.
- **Nunca** agregar co-author de IA en los commits. Siempre hacer commits de manera individual y no mezclar cambios de multiples autores en un solo commit.
