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

    window.onClickItem = function (el) {
        var path = window.location.hash ? getHash() : '';

        if (el.getAttribute('data-filename') === '..') {
            var parent = path.substr(0, path.lastIndexOf('/'));
            window.location.hash = parent || '/';
        } else {
            if (el.getAttribute('data-isdir') === 'true') {
                window.location.hash = `${path}/${el.getAttribute('data-filename')}`;
            } else {
                window.location.href = `${location.protocol}//${window.user}:${window.password}@${location.hostname}:${location.port}/file?path=${path}/${el.getAttribute('data-filename')}`;
                console.log(`${location.protocol}//${window.user}:${window.password}@${location.hostname}:${location.port}/file?path=${path}/${el.getAttribute('data-filename')}`);
            }
        }

        loadData();
        document.getElementById('rechercher').value = '';

    };

    function render() {
        var items = window.items;
        var vFilter = document.getElementById("rechercher").value;
        var itemsFiltered = vFilter ? items.filter(function (item) {
            item.body = item.body ||  '';
            return (item.filename.toLowerCase().indexOf(vFilter.toLowerCase()) !== -1);
        }) : items;
        document.getElementById("items").innerHTML = '';

        if (window.location.hash !== '#/') {
            var el = `
                <article class = "card" data-isdir="false" data-filename=".." onclick="onClickItem(this)" >
                <h3 > .. </h3>
                </article>
                `;
            document.getElementById("items").appendChild(el.toDOM());
        }

        for (var index in itemsFiltered) {
            var item = itemsFiltered[index];
            var ext = item.tags && item.tags.length ? item.tags[0] : '';
            var cls = '';
            var icon = ` <img src = "resources/img/file.svg" alt = "fichier" /> `;
            if (item.isDir) {
                icon = ` <img src = "resources/img/folder-5.svg" alt = "fichier" /> `;
            }
            var el = `
                <article class = "card ${cls}" onclick="onClickItem(this)" data-filename="${item.filename}" data-isdir = "${item.isDir}" >
                ${icon}
        <h3>
                ${item.filename} <span class="size">${item.labelSize}</span> </h3>
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
                window.items = data;
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