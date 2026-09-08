const site = {
  applicationName: "Mombodoro",
  alternateName: "Momotombo",
  downloadUrl: "",
  siteUrl: "",
};

const faq = [
  {
    question: "¿Qué es Mombodoro?",
    answer:
      "Es una aplicación de escritorio para organizar sesiones de enfoque y descanso con un temporizador configurable.",
  },
  {
    question: "¿Puedo personalizar los ciclos?",
    answer:
      "Sí. Puedes ajustar la duración del enfoque, los descansos y la cantidad de ciclos antes de un descanso largo.",
  },
  {
    question: "¿Qué ocurre al terminar una sesión?",
    answer:
      "Mombodoro muestra un aviso y puede enviar una notificación del sistema para indicar el siguiente momento del ciclo.",
  },
];

function setDownloadLinks() {
  const isAvailable = Boolean(site.downloadUrl);

  document.querySelectorAll(".download-link").forEach((link) => {
    if (isAvailable) {
      link.href = site.downloadUrl;
      return;
    }

    link.setAttribute("aria-disabled", "true");
    link.addEventListener("click", (event) => {
      event.preventDefault();
      const status = link.parentElement.querySelector("[data-download-status]");
      status?.scrollIntoView({ behavior: "smooth", block: "center" });
    });
  });
}

function setMetadata() {
  const url = site.siteUrl ? new URL(site.siteUrl) : null;

  if (url) {
    document.querySelector("[data-canonical]").href = url.href;
    document.querySelector("[data-og-url]").content = url.href;
  }

  const data = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "SoftwareApplication",
        name: site.applicationName,
        alternateName: site.alternateName,
        applicationCategory: "ProductivityApplication",
        operatingSystem: "macOS",
        description:
          "Temporizador de enfoque para macOS con ciclos configurables, tareas y avisos discretos.",
        ...(url ? { url: url.href } : {}),
      },
      {
        "@type": "FAQPage",
        mainEntity: faq.map(({ question, answer }) => ({
          "@type": "Question",
          name: question,
          acceptedAnswer: { "@type": "Answer", text: answer },
        })),
      },
    ],
  };

  document.querySelector("#structured-data").textContent = JSON.stringify(data);
}

function setupMenu() {
  const toggle = document.querySelector(".menu-toggle");
  const nav = document.querySelector(".site-nav");

  toggle.addEventListener("click", () => {
    const isOpen = nav.classList.toggle("is-open");
    toggle.setAttribute("aria-expanded", String(isOpen));
  });

  nav.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => {
      nav.classList.remove("is-open");
      toggle.setAttribute("aria-expanded", "false");
    });
  });
}

function setupDemo() {
  const button = document.querySelector("[data-timer-toggle]");
  const timer = document.querySelector("[data-timer]");
  const status = document.querySelector("[data-cycle-status]");
  let seconds = 25 * 60;
  let intervalId = null;

  function renderTimer() {
    const minutes = String(Math.floor(seconds / 60)).padStart(2, "0");
    const remainingSeconds = String(seconds % 60).padStart(2, "0");
    timer.textContent = `${minutes}:${remainingSeconds}`;
  }

  function stopTimer() {
    window.clearInterval(intervalId);
    intervalId = null;
  }

  function startTimer() {
    button.textContent = "Pausar";
    intervalId = window.setInterval(() => {
      seconds -= 1;
      renderTimer();

      if (seconds === 0) {
        stopTimer();
        button.textContent = "Reiniciar";
        status.textContent = "Sesión terminada";
      }
    }, 1000);
  }

  button.addEventListener("click", () => {
    if (seconds === 0) {
      seconds = 25 * 60;
      renderTimer();
      status.textContent = "Primer ciclo de cuatro";
    }

    if (intervalId) {
      stopTimer();
      button.textContent = "Continuar";
      return;
    }

    startTimer();
  });

  document.querySelector("[data-add-task]").addEventListener("click", () => {
    const row = document.createElement("label");
    const checkbox = document.createElement("input");
    const title = document.createElement("span");

    row.className = "task-row";
    checkbox.type = "checkbox";
    title.textContent = "Nueva tarea";
    row.append(checkbox, title);
    document.querySelector(".task-panel").append(row);
  });
}

setDownloadLinks();
setMetadata();
setupMenu();
setupDemo();
