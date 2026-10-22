from flask import Flask, jsonify, request
app = Flask(__name__)

tareas = [
    {
        'id' : 1, 
    'titulo': 'Estudiar Flask', 
    'completada': False
    },
]

@app.route("/todos", methods=["GET"])
def obtener_tareas():
    return jsonify(tareas), 200


@app.route("/todos", methods=["POST"])
def agregar_tarea():

    datos = request.get_json()

    if datos is None:
        return jsonify({"error": "JSON invalido"}), 400

    if "titulo" not in datos:
        return jsonify({"error": "Falta el campo titulo"}), 400

    nueva_tarea = {
        "id": tareas[-1]["id"] + 1 if tareas else 1,
        "titulo": datos["titulo"],
        "completada": datos.get("completada", False)
    }

    tareas.append(nueva_tarea)

    return jsonify(nueva_tarea), 201


@app.route("/todos/<int:id>", methods=["PUT"])
def actualizar_tarea(id):

    datos = request.get_json()

    if datos is None:
        return jsonify({"error": "JSON invalido"}), 400

    for tarea in tareas:

        if tarea["id"] == id:

            if "titulo" in datos:
                tarea["titulo"] = datos["titulo"]

            if "completada" in datos:
                tarea["completada"] = datos["completada"]

            return jsonify(tarea), 200

    return jsonify({"error": "Tarea no encontrada"}), 404

@app.route("/todos/<int:id>", methods=["DELETE"])
def eliminar_tarea(id):

    for tarea in tareas:

        if tarea["id"] == id:
            tareas.remove(tarea)

            return jsonify({
                "mensaje": "Tarea eliminada correctamente"
            }), 200

    return jsonify({
        "error": "Tarea no encontrada"
    }), 404


usuario = {
    "nombre": "Usuario de Prueba",
    "correo": "usuario@ejemplo.com",
    "rut": "12.345.678-5"
}

@app.route("/usuario", methods=["GET"])
def obtener_usuario():
    return jsonify(usuario), 200


@app.route("/login", methods=["POST"])
def validar_login():
    datos = request.get_json(silent=True)

    if not isinstance(datos, dict):
        return jsonify({
            "valido": False,
            "mensaje": "JSON invalido"
        }), 400

    login = datos.get("login")
    password = datos.get("password")

    if login == "demo" and password == "clave123":
        return jsonify({
            "valido": True,
            "mensaje": "Inicio de sesion correcto",
            "usuario": usuario
        }), 200

    return jsonify({
        "valido": False,
        "mensaje": "Credenciales incorrectas"
    }), 401

if __name__ == "__main__":
    app.run(debug=True)