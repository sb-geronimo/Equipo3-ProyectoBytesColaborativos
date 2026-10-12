# Reglas de codificacion

- No usar excepciones genericas para casos de error como Exception, RuntimeExcepcion o BussinessException. En su lugar, se deben crear excepciones personalizadas para cada caso de error específico.
- No manejar respuesta de error en el controlador, sino en un RestControllerAdvice que debe responder con el mensaje de estado y el DTO ErrorResponse.
- **Jamas** usar comentarios en el codigo
- **No user** nombres de variables o metodos genericos como data, info, process, handle, etc. Los nombres deben ser descriptivos y reflejar claramente la funcionalidad que representan.
- **No acoplar** mucha logica en un solo servicio, delegar tareas como manejo de errores, consultas complejas o opciones comunes en Specifications y helpers
- **Usar** Mapper para mapear entidades a DTOs y viceversa usando MapStruct, evitando la logica de mapeo en los controladores o servicios.

# Informacion sobre el proyecto

La descripcion, servicios, endpoints y demas informacion relevante del proyecto se encuentra en el archivo **FleetControl.md**
