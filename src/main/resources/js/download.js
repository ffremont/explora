(function () {
    window.items = [];
    String.prototype.toDOM = function () {
        var d = document,
            i, a = d.createElement("div"),
            b = d.createDocumentFragment();
        a.innerHTML = this;
        while (i = a.firstChild) b.appendChild(i);
        return b;
    };
    window.refresh = function () {
        loadData();
    };
    window.toggleAdd = function (event) {
        if (event && event.defaultPrevented) {
            return;
        }
        if(event){
            event.preventDefault();
        }
        var mask = document.getElementById('add-mask'),
            file = document.getElementById('add-file'),
            folder = document.getElementById('add-folder');
        if (mask.className.indexOf('hide') === -1) {
            // hide adds
            mask.className = "hide";
            file.className = "hide";
            folder.className = "hide";
        } else {
            mask.className = "";
            file.className = "";
            folder.className = "";
        }
    };
    window.creerDossier = function (event) {
        event.preventDefault();
        var val = document.getElementById('add-folder-value').value;
        Modal.close();

        document.getElementById('loader').className = '';
        fetch('/folder?path=' + getHash(), {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    Authorization: 'Basic ' + btoa(window.user + ':' + window.password)
                },
                method: 'POST',
                body: 'name=' + encodeURIComponent(val)
            })
            .then(function (response) {
                if (response.status !== 204) {
                    throw "oups";
                }

                document.getElementById('loader').className = 'hide';
                window.refresh();
            })
            .catch(function () {
                document.getElementById('loader').className = 'hide';
                alert('Echec de la création du répertoire.');
            });
    };
    window.totaluploadprogress = function(totalPercentage){
        var bodyEl = document.querySelector('body');
        if(bodyEl.className.indexOf('uploading') === -1){
            bodyEl.className = bodyEl.className+' uploading';
        }
        
        var lblEl = document.getElementById('uploading-value');
        if(totalPercentage >= 100){
            lblEl.innerText = "100%";
            bodyEl.className = bodyEl.className.replace(' uploading', '');
        }else{
            lblEl.innerText = parseFloat(totalPercentage).toFixed(1)+"%";
        }
    };
    window.addFile = function (event) {
        window.toggleAdd(event);
        Modal.open({
            hideclose: true,
            content: `
                    <form id="dnd-files" action="/files" class="dropzone">
                        
                    </form>
                `,
            draggable: false,
            closeCallback: function(){
                window.refresh();
            }
        });
        new Dropzone("#dnd-files", {
            url: '/files?path=' + getHash() || '/',
            paramName: "file", // The name that will be used to transfer the file
            maxFilesize: 5120, // MB
            dictDefaultMessage: 'Glisser ici vos fichiers',
            headers : {
                Authorization: 'Basic ' + btoa(window.user + ':' + window.password)
            },
            uploadMultiple: true,
            totaluploadprogress : window.totaluploadprogress
        });
        
    };
    window.addFolder = function (event) {
        window.toggleAdd(event);
        Modal.open({
            hideOverlay:false,
            content: `
                    <h2>Créer un dossier</h2>
                    
                    <form onsubmit="creerDossier(event)">
                        <input type="text" required id="add-folder-value"  />
                        <br/>
                        <button type="submit" class="success">Créer</button> <button type="button" onclick="Modal.close()">Annuler</button>
                    </form>
                `,
            draggable: false
        });
    };
    window.goto = function (event, path) {
        event.preventDefault();
        window.location.hash = path;
        loadData();
        document.getElementById('rechercher').value = '';
    }
    window.download = function (event, el) {
        var path = window.location.hash ? getHash() : '';
        window.location.href = `${location.protocol}//${window.user}:${window.password}@${location.hostname}:${location.port}/file?download=true&path=${path}/${el.getAttribute('data-filename')}`;
    };
    window.onClickItem = function (event, el) {
        if (event.defaultPrevented) {
            return;
        }
        var path = window.location.hash ? getHash() : '';
        if (el.getAttribute('data-filename') === '..') {
            var parent = path.substr(0, path.lastIndexOf('/'));
            window.location.hash = parent || '/';
        } else {
            if (el.getAttribute('data-isdir') === 'true') {
                path = path === '/' ? path : path + '/';
                window.location.hash = `${path}${el.getAttribute('data-filename')}`;
            } else {
                return;
                // window.location.href = `${location.protocol}//${window.user}:${window.password}@${location.hostname}:${location.port}/file?path=${path}/${el.getAttribute('data-filename')}`;
            }
        }

        loadData();
        document.getElementById('rechercher').value = '';
    };
    window.supp = function (event, el) {
        event.preventDefault();
        var filename = el.getAttribute('data-filename');
        if (confirm(`Voulez - vous supprimer "${filename}" ?`)) {
            fetch(`/files?path=${getHash()}/${filename}`, {
                    method: 'DELETE',
                    headers: {
                        Accept: 'application/json',
                        Authorization: 'Basic ' + btoa(window.user + ':' + window.password)
                    }
                }).then(function (response) {
                    if (response.status !== 200) {
                        throw "oups";
                    }

                    window.refresh();
                })
                .catch(function () {
                    alert('Echec de la suppression de l\'élément.');
                })
        }
    };

    function render() {
        var items = window.items;
        var vFilter = document.getElementById("rechercher").value;
        var itemsFiltered = vFilter ? items.filter(function (item) {
            item.body = item.body ||  '';
            return (item.filename.toLowerCase().indexOf(vFilter.toLowerCase()) !== -1);
        }) : items;
        document.getElementById("items").innerHTML = '';
        if (window.location.hash &&  (window.location.hash !== '#/')) {
            var el = `
                <article class = "card" data-isdir = "false" data-filename = ".." onclick = "onClickItem(event, this)">
                <h3> .. </h3>
                </article>
                `;
            document.getElementById("items").appendChild(el.toDOM());
        }

        var path = window.location.hash ? getHash() : '';
        for (var index in itemsFiltered) {
            var item = itemsFiltered[index];
            var ext = item.tags && item.tags.length ? item.tags[0] : '';
            var extension = item.filename.indexOf('.') === -1 ? 'file' : item.filename.substr( item.filename.lastIndexOf('.')).replace('.', '').toLowerCase();
            var cls = '';
                        
            var href = `${location.protocol}//${location.hostname}:${location.port}/file?path=${path}/${item.filename}`;
            
            var btnVoir = `<a href="${href}" target="_blank"><img src = "resources/img/view.svg" title = "Consulter le fichier"  /></a>`;
            var icon = ` <img src = "resources/img/${extension}.svg" onerror="this.src='resources/img/file.svg'" alt= "fichier"/> `;
            var downloadEl = ` <img onclick = "download(event,this)" title = "Télécharger" src = "resources/img/download.svg" data-filename = "${item.filename}" /> `;
            if (item.isDir) {
                icon = ` <img src = "resources/img/folder-5.svg" alt = "fichier" /> `;
                downloadEl = '';
                btnVoir='';
            }
            var el = `
                <article class = "card ${cls}" onclick = "onClickItem(event, this)" data-filename = "${item.filename}" data-isdir = "${item.isDir}">
                ${icon}
        <h3>
                ${item.filename} <span class = "size"> ${item.labelSize} </span> 

                
                <img src = "resources/img/garbage.svg" title = "Supprimer" onclick = "supp(event, this)" data-filename = "${item.filename}" />
                
                ${downloadEl}
                ${btnVoir}

        </h3>
                </article>
                `;
            document.getElementById("items").appendChild(el.toDOM());
        }
    };
    document.getElementById('rechercher').addEventListener('keyup', render);

    function loadData() {
        document.getElementById('loader').className = '';
        fetch('/files?path=' + getHash() || '/', {
                headers: {
                    Accept: 'application/json',
                    Authorization: 'Basic ' + btoa(window.user + ':' + window.password)
                }
            })
            .then(function (response) {
                if (response.status !== 200) {
                    throw "oups";
                } else {
                    return response.json();
                }
            })
            .then(function (data) {
                document.getElementById('loader').className = 'hide';
                window.items = data.files;
            // barre d'info sur l'espace disque
                var gaugeEl = document.getElementById('space-gauge');
                var gaugeSpanEl = document.querySelector('#space-gauge span');
                var usedGo = parseFloat((data.totalSpace - data.freeSpace) / (1024*1024*1024)).toFixed(3);
                var totalSpaceGo = parseFloat((data.totalSpace) / (1024*1024*1024)).toFixed(3);
                gaugeEl.title = `Espace occupé à ${data.ratioSpace} %, soit ${usedGo} Go sur ${totalSpaceGo} Go`;
                gaugeSpanEl.className = 'blue';
                if((data.ratioSpace > 75) && (data.ratioSpace < 92)){
                    gaugeSpanEl.className = 'yellow';
                } else if(data.ratioSpace >= 92){
                    gaugeSpanEl.className = 'red';
                }
                gaugeSpanEl.setAttribute('style', `width: ${data.ratioSpace}%`);
                gaugeEl.className = gaugeEl.className.replace(' hide', '');
            
                render();
            })
            .catch(function (e) {
                document.getElementById('loader').className = 'hide';
                alert('Erreur lors de la récupération des fichiers');
            });
    }

    function getHash() {
        return window.location.hash ? window.location.hash.replace('#', '') : '';
    }

    function submit(e) {
        e.preventDefault();
        window.user = document.getElementById('u').value;
        window.password = document.getElementById('p').value;
        document.getElementById('login').className = 'hide';
        loadData();
    }
    document.getElementById('login-form').addEventListener('submit', submit);
}());