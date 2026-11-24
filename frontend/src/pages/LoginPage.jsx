import { useEffect, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { obtenerDemoInfo } from "../services/demoService";

export function LoginPage() {
  const { estaAutenticado, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [demoInfo, setDemoInfo] = useState(null);
  const [demoCargando, setDemoCargando] = useState(null);

  useEffect(() => {
    let activo = true;
    obtenerDemoInfo()
      .then((info) => {
        if (activo && info?.habilitado) {
          setDemoInfo(info);
        }
      })
      .catch(() => {
        // Si /demo/info falla (por ejemplo backend sin el modo demo),
        // simplemente ocultamos la seccion demo.
      });
    return () => {
      activo = false;
    };
  }, []);

  if (estaAutenticado) {
    return <Navigate to="/inicio" replace />;
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      await login(username.trim(), password);
      navigate("/inicio", { replace: true });
    } catch (err) {
      const status = err.response?.status;
      const mensajeApi = err.response?.data?.mensaje;
      if (status === 423) {
        setError(mensajeApi ?? "La cuenta esta bloqueada.");
      } else if (status === 401) {
        setError(mensajeApi ?? "Usuario o contraseña incorrectos.");
      } else {
        setError("Error al iniciar sesion. Intenta de nuevo.");
      }
    } finally {
      setCargando(false);
    }
  }

  async function entrarComoDemo(usernameDemo) {
    if (!demoInfo?.password) return;
    setError(null);
    setDemoCargando(usernameDemo);
    try {
      await login(usernameDemo, demoInfo.password);
      navigate("/inicio", { replace: true });
    } catch (err) {
      const mensajeApi = err.response?.data?.mensaje;
      setError(mensajeApi ?? "No se pudo entrar con la cuenta demo.");
    } finally {
      setDemoCargando(null);
    }
  }

  return (
    <div className="login-page">
      <div className="login-contenedor">
        <form className="login-form" onSubmit={onSubmit}>
          <h1>Sistema de Correspondencia</h1>
          <p className="login-subtitle">Ingresa tus credenciales</p>

          <label>
            Usuario
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </label>

          <label>
            Contraseña
            <div className="campo-password">
              <input
                type={passwordVisible ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
              <button
                type="button"
                className="btn-visor"
                onClick={() => setPasswordVisible((v) => !v)}
                aria-label={
                  passwordVisible ? "Ocultar contraseña" : "Mostrar contraseña"
                }
              >
                {passwordVisible ? "Ocultar" : "Mostrar"}
              </button>
            </div>
          </label>

          {error && <div className="alerta alerta-error">{error}</div>}

          <button type="submit" disabled={cargando}>
            {cargando ? "Ingresando..." : "Ingresar"}
          </button>
        </form>

        {demoInfo && (
          <section className="demo-panel" aria-label="Accesos de demostracion">
            <h2>Probar la demo</h2>
            <p className="demo-panel-intro">
              Entra con un clic. Los datos se reinician cada noche.
            </p>
            <div className="demo-tarjetas">
              {demoInfo.usuarios.map((u) => (
                <button
                  type="button"
                  key={u.username}
                  className="demo-tarjeta"
                  onClick={() => entrarComoDemo(u.username)}
                  disabled={demoCargando !== null}
                >
                  <span className="demo-tarjeta-rol">{u.rol}</span>
                  <span className="demo-tarjeta-descripcion">{u.descripcion}</span>
                  <span className="demo-tarjeta-usuario">
                    {demoCargando === u.username ? "Entrando..." : u.username}
                  </span>
                </button>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
